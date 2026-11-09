#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
أداة إدارة قنوات تطبيق Curated YouTube TV ومزامنتها تلقائياً مع GitHub.
الاستخدام:
    1. لإضافة قناة عبر رابط القناة أو اسم المعرف (Handle) أو رابط أي فيديو:
       python add_channel.py https://www.youtube.com/watch?v=VIDEO_ID
       python add_channel.py @ChannelName

    2. لإضافة عدة قنوات دفعة واحدة (روابط قنوات أو فيديوهات):
       python add_channel.py @channel1 https://youtu.be/xxx https://www.youtube.com/@channel3

    3. لإضافة قنوات من ملف نصي (كل رابط أو معرف في سطر):
       python add_channel.py channels.txt

    4. لمزامنة التغييرات تلقائياً إلى GitHub، أضف --push:
       python add_channel.py channels.txt --push
       python add_channel.py --push
"""

import sys
import json
import urllib.request
import re
import os
import subprocess

try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

def push_to_github():
    print("\n🚀 جاري رفع التحديثات إلى GitHub...")
    try:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        subprocess.run(["git", "add", "channels.json", "app/src/main/assets/channels.json"], cwd=script_dir, check=True)
        diff_res = subprocess.run(["git", "diff", "--staged", "--quiet"], cwd=script_dir)
        if diff_res.returncode != 0:
            subprocess.run(["git", "commit", "-m", "تحديث قائمة القنوات"], cwd=script_dir, check=True)
            subprocess.run(["git", "push"], cwd=script_dir, check=True)
            print("✅ تم رفع التحديثات بنجاح إلى مستودع GitHub! ستظهر على التلفاز فوراً.")
        else:
            print("ℹ️ لا توجد تغييرات جديدة لرفعها.")
    except Exception as e:
        print(f"⚠️ تعذر عمل git push تلقائياً: {e}")
        print("يمكنك تشغيل: git commit -am 'update' ثم git push يدوياً.")

def fetch_channel_info(target):
    target = target.strip()
    if not target:
        return None

    # هل الرابط هو رابط فيديو؟ (watch أو youtu.be أو shorts)
    is_video = "youtu.be/" in target or "/watch" in target or "/shorts/" in target

    if is_video:
        url = target
    elif not target.startswith("http"):
        if target.startswith("@"):
            url = f"https://www.youtube.com/{target}"
        elif target.startswith("UC") and len(target) == 24:
            url = f"https://www.youtube.com/channel/{target}"
        else:
            url = f"https://www.youtube.com/@{target}"
    else:
        url = target

    label = "الفيديو لاستخراج القناة" if is_video else "القناة"
    print(f"\n🔍 جاري فحص {label}: {url} ...")
    headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
    req = urllib.request.Request(url, headers=headers)

    try:
        with urllib.request.urlopen(req, timeout=12) as resp:
            html = resp.read().decode('utf-8', errors='ignore')

            # استخراج Channel ID
            m_id = re.search(r'"channelId":"(UC[a-zA-Z0-9_-]{22})"', html)
            if not m_id:
                m_id = re.search(r'itemprop="channelId" content="(UC[a-zA-Z0-9_-]{22})"', html)
            if not m_id:
                m_id = re.search(r'"externalId":"(UC[a-zA-Z0-9_-]{22})"', html)

            if not m_id:
                print(f"❌ تعذر العثور على معرّف القناة (Channel ID) من: {url}")
                return None

            channel_id = m_id.group(1)

            # استخراج اسم القناة ورابطها ومعرفها
            if is_video:
                m_author = re.search(r'<link itemprop="name" content="([^"]+)">', html) or re.search(r'"author":"([^"]+)"', html)
                name = m_author.group(1) if m_author else "قناة يوتيوب"

                m_owner = re.search(r'"ownerProfileUrl":"([^"]+)"', html) or re.search(r'<link itemprop="url" href="([^"]+)"', html)
                if m_owner:
                    channel_url = m_owner.group(1).replace("http://", "https://")
                    m_handle = re.search(r'/(@[^/?#]+)', channel_url)
                    handle = m_handle.group(1) if m_handle else f"@{channel_id}"
                else:
                    channel_url = f"https://www.youtube.com/channel/{channel_id}"
                    handle = f"@{channel_id}"
            else:
                m_title = re.search(r'<meta property="og:title" content="([^"]+)">', html)
                name = m_title.group(1) if m_title else "قناة يوتيوب"

                m_handle = re.search(r'"canonicalBaseUrl":"/(@[^"]+)"', html)
                handle = m_handle.group(1) if m_handle else (url.split("/")[-1] if "@" in url else f"@{channel_id}")
                channel_url = f"https://www.youtube.com/{handle}" if handle.startswith("@") else url

            return {
                "name": name,
                "handle": handle,
                "channel_id": channel_id,
                "url": channel_url,
                "category": "محتوى مخصص",
                "enabled": True
            }
    except Exception as e:
        print(f"❌ فشل جلب البيانات من ({url}): {e}")
        return None

def main():
    if len(sys.argv) < 2:
        print("❌ الرجاء تحديد القنوات أو روابط الفيديوهات، أمثلة:")
        print("   1. إضافة قناة عبر رابط فيديو:")
        print("      python add_channel.py https://www.youtube.com/watch?v=xxxx")
        print("   2. إضافة عدة قنوات أو فيديوهات:")
        print("      python add_channel.py @channel1 https://youtu.be/xxxx")
        print("   3. قراءة من ملف نصي:")
        print("      python add_channel.py channels.txt --push")
        sys.exit(1)

    do_push = "--push" in sys.argv
    args = [arg for arg in sys.argv[1:] if arg != "--push"]

    if not args and do_push:
        push_to_github()
        sys.exit(0)

    # جمع المدخلات من المعاملات أو من ملفات نصية
    targets = []
    for arg in args:
        if os.path.isfile(arg):
            print(f"📄 جاري قراءة الروابط من الملف: {arg}")
            with open(arg, "r", encoding="utf-8") as f:
                for line in f:
                    clean = line.strip()
                    if clean and not clean.startswith("#"):
                        targets.append(clean)
        else:
            targets.append(arg)

    if not targets:
        print("⚠️ لم يتم العثور على أي روابط للمعالجة.")
        sys.exit(0)

    print(f"📋 إجمالي الروابط المطلوب معالجتها: {len(targets)}")

    script_dir = os.path.dirname(os.path.abspath(__file__))
    json_paths = [
        os.path.join(script_dir, "channels.json"),
        os.path.join(script_dir, "app", "src", "main", "assets", "channels.json")
    ]

    added_channels = []
    for target in targets:
        info = fetch_channel_info(target)
        if info:
            print(f"✅ تم التعرف على القناة: {info['name']} ({info['handle']}) | ID: {info['channel_id']}")
            added_channels.append(info)

    if not added_channels:
        print("\n⚠️ لم تتم إضافة أي قنوات.")
        sys.exit(1)

    # حفظ القنوات في الملفات
    for json_path in json_paths:
        if not os.path.exists(json_path):
            continue

        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        existing_ids = {c.get("channel_id") for c in data.get("channels", [])}
        new_count = 0

        for ch in added_channels:
            if ch["channel_id"] not in existing_ids:
                data.setdefault("channels", []).append(ch)
                existing_ids.add(ch["channel_id"])
                new_count += 1

        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

        print(f"🎉 تم تحديث {os.path.basename(json_path)}: تمت إضافة {new_count} قناة جديدة.")

    if do_push:
        push_to_github()

if __name__ == "__main__":
    main()
