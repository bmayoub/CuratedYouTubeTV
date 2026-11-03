# 📺 Curated YouTube TV (تطبيق يوتيوب مخصص لشاشات Google TV)

تطبيق رسمي مبني بتقنية **Native Kotlin + Jetpack Compose for TV** لشاشات **Google TV / Android TV**.  
يعمل بنظام تصفية القنوات المخصصة (Curated Channels)، حيث يعرض حصرياً الفيديوهات الصادرة من القنوات التي تحددها بنفسك دون أي محتوى عشوائي أو اقتراحات خارجية.

---

## 🗂️ هيكل ملف القنوات الخارجي (`channels.json`)

يمكنك فتح وتعديل ملف [`channels.json`](channels.json) في أي وقت لإضافة أو حذف قنوات:

```json
{
  "version": 1,
  "app_name": "Curated YouTube TV",
  "channels": [
    {
      "name": "سكتش | Sketch",
      "handle": "@worldbysketch",
      "channel_id": "UCr4xUqTzdvZ8U8PcEDL_YBg",
      "url": "https://www.youtube.com/@worldbysketch",
      "category": "معرفة ووثائقيات",
      "enabled": true
    },
    {
      "name": "قصص مدار | Qisas Madar",
      "handle": "@Qisas_Madar",
      "channel_id": "UC6t0GlogFquiOyriK98hyBA",
      "url": "https://www.youtube.com/@Qisas_Madar",
      "category": "قصص وتاريخ",
      "enabled": true
    }
  ]
}
```

### 💡 كيف تضيف قناة جديدة؟
1. ادخل على رابط القناة في يوتيوب.
2. احصل على معرّف القناة `channel_id` (يبدأ بـ `UC` ومكون من 24 حرفاً).
3. أضف القناة في قائمة `"channels"` داخل ملف `channels.json`.
4. يمكنك وضع ملف `channels.json` مباشرة في مجلد `Download` في التلفاز عبر أي مدير ملفات (مثل **X-plore File Manager** على شاشة التلفاز)، وسيقوم التطبيق بقراءته فوراً دون الحاجة لإعادة تثبيت الـ APK!

---

## 🎮 التحكم بالريموت كونترول (D-Pad Remote):
* **الأسهم (أعلى / أسفل / يمين / يسار):** التنقل السلس بين الفيديوهات والقائمة الجانبية (Sidebar).
* **زر الوسط / الاختيار (OK / Center):** فتح الفيديو وبدء التشغيل، أو إيقاف/استئناف الفيديو أثناء المشاهدة.
* **سهم يمين أثناء المشاهدة:** تقديم 10 ثوانٍ للأمام.
* **سهم يسار أثناء المشاهدة:** تأخير 10 ثوانٍ للخلف.
* **زر الرجوع (Back):** الخروج من مشغل الفيديو والعودة لشاشة التصفح.

---

## 🔨 طريقة البناء والتثبيت على التلفاز (Build APK):

### 1. عبر Android Studio:
1. افتح برنامج **Android Studio**.
2. اختر **Open** وحدد المجلد: `E:\CuratedYouTubeTV`.
3. اضغط من القائمة العلوية على:  
   `Build` $\rightarrow$ `Build Bundle(s) / APK(s)` $\rightarrow$ `Build APK(s)`.
4. ستجد ملف الـ APK جاهزاً في:  
   `E:\CuratedYouTubeTV\app\build\outputs\apk\debug\app-debug.apk`.

### 2. تثبيت الـ APK على شاشة Google TV:
* **الطريقة الأسهل:** ثبت تطبيق **Send files to TV** من متجر Google TV على الشاشة وعلى هاتفك، وأرسل ملف الـ `APK` وثبته مباشرة.
* **عبر فلاشة USB:** انسخ ملف الـ `APK` وملف `channels.json` على فلاشة USB وركبها في الشاشة وثبت التطبيق عبر تطبيق إدارة الملفات.

