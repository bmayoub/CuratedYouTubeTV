#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
أداة إضافة قنوات وبثوث مباشرة إلى قائمة playlist.m3u ورفعها تلقائياً إلى GitHub
الاستخدام:
    python add_stream.py "اسم القناة" "رابط_البث"
    python add_stream.py "اسم القناة" "رابط_البث" --push
"""

import sys
import os
import subprocess

try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

if len(sys.argv) < 3:
    print("❌ طريقة الاستخدام:")
    print('   .\\add_stream "اسم القناة" \'رابط_البث\'')
    print('   .\\add_stream "اسم القناة" \'رابط_البث\' -Push')
    print('مثال:')
    print('   .\\add_stream "سكاي نيوز عربية" \'https://.../playlist.m3u8\' -Push')
    sys.exit(1)

do_push = False
args = []
for arg in sys.argv[1:]:
    if arg.lower() in ["--push", "-push", "-p"]:
        do_push = True
    else:
        args.append(arg)

if len(args) < 2:
    print("❌ الرجاء تحديد اسم القناة والرابط معاً.")
    sys.exit(1)

name = args[0].strip().strip('"').strip("'")
url = args[1].strip().strip('"').strip("'")

script_dir = os.path.dirname(os.path.abspath(__file__))
playlist_path = os.path.join(script_dir, "playlist.m3u")

# التأكد من وجود ملف playlist.m3u
if not os.path.exists(playlist_path):
    with open(playlist_path, "w", encoding="utf-8") as f:
        f.write("#EXTM3U\n")

# التحقق مما إذا كان الرابط موجوداً مسبقاً
with open(playlist_path, "r", encoding="utf-8") as f:
    content = f.read()

if url in content:
    print(f"ℹ️ القناة أو الرابط موجود مسبقاً في القائمة: {name}")
else:
    with open(playlist_path, "a", encoding="utf-8") as f:
        # إضافة سطر فاصل إذا لزم الأمر
        if not content.endswith("\n"):
            f.write("\n")
        f.write(f'\n#EXTINF:-1 tvg-name="{name}" group-title="بث مباشر",{name}\n')
        f.write(f'{url}\n')
    print(f"✅ تمت إضافة [{name}] إلى ملف playlist.m3u بنجاح!")

# الرفع إلى GitHub تلقائياً إذا طُلب ذلك
if do_push:
    print("🚀 جاري رفع التحديثات إلى GitHub...")
    try:
        subprocess.run(["git", "add", "playlist.m3u"], cwd=script_dir, check=True)
        diff_res = subprocess.run(["git", "diff", "--staged", "--quiet"], cwd=script_dir)
        if diff_res.returncode != 0:
            commit_msg = f"feat: إضافة قناة {name} إلى قائمة البث"
            subprocess.run(["git", "commit", "-m", commit_msg], cwd=script_dir, check=True)
            subprocess.run(["git", "push"], cwd=script_dir, check=True)
            print("🎉 تم رفع القائمة المحدثة بنجاح إلى GitHub!")
            print("🔗 الرابط القصير المحدث: tinyurl.com/ayoub-tv")
        else:
            print("ℹ️ لم يتم العثور على تغييرات جديدة لرفعها.")
    except Exception as e:
        print(f"⚠️ حدث خطأ أثناء الرفع إلى GitHub: {e}")
        print("يمكنك تشغيل: git commit -am 'update' ثم git push يدوياً.")
else:
    print("💡 لم يتم الرفع إلى GitHub بعد. لرفعها الآن شغّل: git commit -am 'update'; git push")
    print("💡 أو أضف -Push في نهاية الأمر المرة القادمة ليتم الرفع تلقائياً!")
