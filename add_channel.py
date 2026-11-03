#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
أداة إدارة قنوات تطبيق Curated YouTube TV ومزامنتها تلقائياً مع GitHub.
الاستخدام:
    1. لإضافة قناة وتحديث الملف:
       python add_channel.py https://www.youtube.com/@ChannelName

    2. لإضافة قناة ومزامنتها فوراً إلى GitHub:
       python add_channel.py https://www.youtube.com/@ChannelName --push

    3. لمزامنة التغييرات الحالية مع GitHub مباشرة:
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
        # التحقق مما إذا كانت هناك تغييرات للالتزام بها
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

if len(sys.argv) < 2:
    print("❌ الرجاء تمرير رابط أو اسم القناة، مثال:")
    print("   python add_channel.py https://www.youtube.com/@aljazeeradocumentary")
    print("   python add_channel.py https://www.youtube.com/@aljazeeradocumentary --push")
    sys.exit(1)

do_push = "--push" in sys.argv
args = [arg for arg in sys.argv[1:] if arg != "--push"]

if not args and do_push:
    push_to_github()
    sys.exit(0)

target = args[0].strip()
if not target.startswith("http"):
    if target.startswith("@"):
        target = f"https://www.youtube.com/{target}"
    else:
        target = f"https://www.youtube.com/@{target}"

print(f"🔍 جاري فحص القناة: {target} ...")

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
req = urllib.request.Request(target, headers=headers)

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
            print("❌ تعذر العثور على معرّف القناة (Channel ID). تأكد من صحة الرابط.")
            sys.exit(1)

        channel_id = m_id.group(1)

        # استخراج اسم القناة
        m_title = re.search(r'<meta property="og:title" content="([^"]+)">', html)
        name = m_title.group(1) if m_title else "قناة يوتيوب"

        # استخراج الهاندل
        m_handle = re.search(r'"canonicalBaseUrl":"/(@[^"]+)"', html)
        handle = m_handle.group(1) if m_handle else target.split("/")[-1]

        print(f"✅ تم التعرف على القناة:")
        print(f"   - الاسم: {name}")
        print(f"   - المعرف: {channel_id}")
        print(f"   - الهاندل: {handle}")

        # تحديث ملف channels.json
        script_dir = os.path.dirname(os.path.abspath(__file__))
        json_paths = [
            os.path.join(script_dir, "channels.json"),
            os.path.join(script_dir, "app", "src", "main", "assets", "channels.json")
        ]

        for json_path in json_paths:
            if not os.path.exists(json_path):
                continue

            with open(json_path, "r", encoding="utf-8") as f:
                data = json.load(f)

            # التحقق إذا كانت القناة مضافة مسبقاً
            exists = any(c.get("channel_id") == channel_id for c in data.get("channels", []))
            if exists:
                print(f"ℹ️ القناة موجودة مسبقاً في: {os.path.basename(json_path)}")
                continue

            data.setdefault("channels", []).append({
                "name": name,
                "handle": handle,
                "channel_id": channel_id,
                "url": target,
                "category": "محتوى مخصص",
                "enabled": True
            })

            with open(json_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)

            print(f"🎉 تمت إضافة القناة بنجاح إلى: {json_path}")

    if do_push:
        push_to_github()

except Exception as e:
    print(f"❌ حدث خطأ أثناء الاتصال: {e}")
    sys.exit(1)
