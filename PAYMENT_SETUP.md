# Thanh toan QR that qua TPBank va SePay

Ung dung dung VietQR de tao ma chuyen khoan va SePay de xac nhan giao dich that qua webhook. Thong tin tai khoan TPBank hien giu nguyen trong `web/WEB-INF/web.xml`:

- Bank code: `970423`
- So tai khoan: `88802102004`
- Ten tai khoan: `TRUONG KHANH DONG`

## 1. Tao tai khoan va lien ket TPBank tren SePay

1. Dang nhap tai `https://my.sepay.vn`.
2. Neu chua co tai khoan, dang ky tai khoan SePay va xac minh email/so dien thoai.
3. Mo phan quan ly tai khoan ngan hang, chon them tai khoan va lien ket tai khoan TPBank dang nhan tien.
4. Hoan tat OTP/xac minh theo yeu cau cua SePay.
5. Kiem tra tai khoan da lien ket co dung so tai khoan `88802102004` va dung bank TPBank.

## 2. Tao API key cho webhook

1. Mo menu **Webhooks** hoac truy cap `https://my.sepay.vn/webhooks`.
2. Chon **+ Them webhook**.
3. O buoc **Thong tin co ban**:
	- Dat ten, vi du `RestaurantManagement payment`.
	- Chon su kien **Co tien vao**.
	- Nhap URL public: `https://<ten-mien-cua-nha-hang>/RestaurantManagement/webhooks/sepay`.
4. O buoc **Tai khoan va bo loc**, chon tai khoan TPBank da lien ket. Neu co bo loc tien to, khong gioi han sai tien to; ung dung se tu kiem tra noi dung `RM-TABLE-<tableId>`.
5. O buoc **Bao mat**, chon **API Key**.
6. Tao/generate API key, sao chep ngay gia tri key va luu trong password manager. Day chinh la gia tri cua `SEPAY_WEBHOOK_TOKEN`.
7. Hoan tat tao webhook. Khong dua API key vao JSP, JavaScript, Git hoac file `web.xml`.

## 3. Cau hinh bien moi truong

Dat hai bien moi truong tren may chu chay Tomcat. Khong ghi token vao source code:

```powershell
$env:SEPAY_WEBHOOK_TOKEN = "<token-do-SePay-cap>"
$env:SEPAY_ACTOR_USER_ID = "<user-id-cua-tai-khoan-quan-ly>"
```

`SEPAY_WEBHOOK_TOKEN` la API key vua tao tren SePay.

`SEPAY_ACTOR_USER_ID` khong phai ID cua SePay. Day la `user_id` cua mot tai khoan trong database cua ung dung, dung de ghi nhan nguoi xu ly thanh toan. Co the tra cuu bang SQL:

```sql
SELECT user_id, username, email, role_id
FROM Users
WHERE is_active = 1 AND is_deleted = 0;
```

Chon user nhan vien quan ly thanh toan, sau do dat hai bien tren may chu chay Tomcat. Sau khi dat bien, khoi dong lai Tomcat.

Vi du tren Windows PowerShell:

```powershell
$env:SEPAY_WEBHOOK_TOKEN = "API_KEY_VUA_TAO_TREN_SEPAY"
$env:SEPAY_ACTOR_USER_ID = "ID_USER_TRONG_DATABASE"
```

Neu muon bien ton tai sau khi khoi dong lai may, dung `setx` trong cua so moi, sau do restart Tomcat:

```powershell
setx SEPAY_WEBHOOK_TOKEN "API_KEY_VUA_TAO_TREN_SEPAY"
setx SEPAY_ACTOR_USER_ID "ID_USER_TRONG_DATABASE"
```

## 4. Cau hinh webhook tren SePay

URL webhook cua ung dung la:

```text
https://<ten-mien-cua-nha-hang>/RestaurantManagement/webhooks/sepay
```

SePay se tu gui header:

```text
Authorization: Apikey API_KEY_VUA_TAO_TREN_SEPAY
```

Endpoint chi chap nhan giao dich co noi dung chua ma dang `RM-TABLE-<tableId>` va so tien dung voi bill.

## 5. Chay local voi localhost va ngrok

SePay khong the goi truc tiep vao `localhost`, `127.0.0.1` hoac IP LAN nhu `192.168.x.x`. Neu muon Tomcat van chay tren may local, dung tunnel HTTPS.

1. Cai ngrok tu `https://ngrok.com/download`.
2. Dang ky/dang nhap tai khoan ngrok tai `https://dashboard.ngrok.com/signup`.
3. Mo trang `https://dashboard.ngrok.com/get-started/your-authtoken`, sao chep authtoken va chay lenh sau trong CMD hoac PowerShell:

```powershell
ngrok config add-authtoken YOUR_NGROK_AUTHTOKEN
```

Lenh nay chi can chay mot lan tren may. Khong gui authtoken cho nguoi khac.

4. Khoi dong ung dung tren Tomcat, vi du tai:

```text
http://localhost:8080/RestaurantManagement
```

5. Mo cua so terminal moi va chay:

```powershell
ngrok http 8080
```

6. Ngrok se hien mot URL, vi du:

```text
Forwarding https://abc123.ngrok-free.app -> http://localhost:8080
```

7. Trong SePay, dat webhook URL thanh:

```text
https://abc123.ngrok-free.app/RestaurantManagement/webhooks/sepay
```

8. Giu cua so ngrok dang chay. Dong ngrok la SePay khong con goi duoc webhook. Moi lan khoi dong lai ngrok mien phi, hostname co the doi va phai cap nhat URL webhook tren SePay.
9. Mo trang `https://abc123.ngrok-free.app/RestaurantManagement` de test bang tunnel, hoac van mo bang localhost tren may phat trien. Ca hai deu vao cung Tomcat local.

Neu thay `ERR_NGROK_4018`, chua dang nhap hoac chua chay `ngrok config add-authtoken`. Loi nay xuat hien truoc khi ngrok kiem tra port, nen doi `8080` sang `9999` cung khong khac phuc.

Co the xem request SePay gui den tunnel tai:

```text
http://127.0.0.1:4040
```

## 6. Cau hinh QR de khach khong can dung chung Wi-Fi

QR ban phai tro den mot URL public qua HTTPS. May tinh chay Tomcat khong tu dong
tro thanh website Internet chi vi dien thoai va laptop cung mang Wi-Fi. Co hai cach:

### Cach A: chay tren hosting/server public (khuyen nghi)

Dat ung dung va database tren mot server co ten mien, sau do dat bien moi truong
`QR_BASE_URL` tren may chay Tomcat:

```powershell
$env:QR_BASE_URL = "https://order.example.com/RestaurantManagement"
```

URL nay phai truy cap duoc bang 4G/5G o ngoai nha hang. Khoi dong lai Tomcat sau
khi dat bien. QR moi tao trong trang **So do ban** se luon dung URL nay, ke ca khi
nhan vien mo trang quan tri bang `localhost`.

### Cach B: dung tunnel HTTPS trong luc thu nghiem

Neu Tomcat van chay tren laptop, dung ngrok (hoac Cloudflare Tunnel) de tao URL
public tam thoi:

```powershell
ngrok http 8080
```

Neu ngrok hien:

```text
Forwarding https://abc123.ngrok-free.app -> http://localhost:8080
```

thi dat:

```powershell
$env:QR_BASE_URL = "https://abc123.ngrok-free.app/RestaurantManagement"
```

Khoi dong lai Tomcat, mo lai trang **So do ban**, sau do in/tai lai QR. Phai giu
ngrok dang chay; URL mien phi co the doi sau moi lan khoi dong. Cach nay phu hop
demo/test, khong nen la giai phap van hanh lau dai.

Khong dat `QR_BASE_URL` la `localhost`, `127.0.0.1` hoac `192.168.x.x` neu khach
can quet bang mang di dong. Cac gia tri do chi dung cho test noi bo.

## 7. Cau hinh QR khi dien thoai truy cap local

`localhost` tren dien thoai la chinh dien thoai, khong phai may tinh chay Tomcat. Neu dien thoai va may tinh cung Wi-Fi:

1. Tim IPv4 cua may tinh bang `ipconfig`, vi du `192.168.1.25`.
2. Cho phep port Tomcat qua Windows Firewall neu can.
3. Dat `qrBaseUrl` trong `web/WEB-INF/web.xml` thanh:

```xml
<param-value>http://192.168.1.25:8080/RestaurantManagement</param-value>
```

4. Dien thoai truy cap `http://192.168.1.25:8080/RestaurantManagement`.

Neu muon dien thoai truy cap qua ngrok, co the dat `QR_BASE_URL` thanh URL ngrok
nhu o tren. Cach cu van hoat dong bang cach dat `qrBaseUrl` trong `web.xml`:

```xml
<param-value>https://abc123.ngrok-free.app/RestaurantManagement</param-value>
```

Khong dat `qrBaseUrl` la `http://localhost:8080/...` khi QR duoc quet bang dien thoai.

## 8. Kiem tra ket noi

1. Trong trang chi tiet webhook, dung **Gui thu/Test send** de gui payload mau.
2. Xem ket qua trong **Nhat ky webhooks**. Ung dung phai tra HTTP 200 va JSON `{"success":true}` khi payload dung ma ban va so tien.
3. Tao mot bill test, mo QR, chuyen mot khoan dung so tien va dung noi dung co ma `RM-TABLE-<tableId>`.
4. Xem log webhook tren SePay va log Tomcat neu trang thai QR khong cap nhat.

## 9. Kiem tra truoc khi thu tien

Man hinh thanh toan chi bat QR khi du ca hai bien moi truong va thong tin tai khoan. Khi khach chuyen khoan, giu man hinh QR mo de webhook cap nhat trang thai; khong xac nhan thu cong khi chua thay trang thai da thanh toan.
