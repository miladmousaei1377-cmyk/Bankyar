# بانک‌یار

مدیریت هوشمند مالی شخصی برای اندروید

## ساخت APK امضاشده (Release)

### ۱. ساخت فایل Keystore

```bash
keytool -genkey -v -keystore release-key.jks \
  -alias bankyar \
  -keyalg RSA -keysize 2048 -validity 10000
```

### ۲. ایجاد فایل `keystore.properties`

فایل `keystore.properties.example` را کپی کرده و مقادیر واقعی را جایگزین کنید:

```bash
cp keystore.properties.example keystore.properties
```

سپس فایل را ویرایش کنید:
```
storeFile=../release-key.jks
storePassword=رمز_keystore_شما
keyAlias=bankyar
keyPassword=رمز_key_شما
```

> **مهم:** هرگز فایل `keystore.properties` یا `*.jks` را commit نکنید!

### ۳. ساخت خودکار از طریق GitHub Actions

برای استفاده از CI، این Secrets را در تنظیمات ریپو GitHub اضافه کنید:

| Secret | مقدار |
|--------|-------|
| `KEYSTORE_BASE64` | خروجی `base64 release-key.jks` |
| `KEYSTORE_PASSWORD` | رمز keystore |
| `KEY_ALIAS` | نام alias (مثلاً `bankyar`) |
| `KEY_PASSWORD` | رمز key |

برای تبدیل jks به base64:
```bash
base64 -i release-key.jks | tr -d '\n'
```
