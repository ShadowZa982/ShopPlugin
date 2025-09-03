# 📦 Shopee – Plugin Shop Tùy Biến Cao

Phiên bản: 1.0
Tác giả: **Fox Studio**
API: S**pigot/Paper 1.20**
Mô tả:
> Shopee là một plugin shop nâng cao, hỗ trợ đa loại tiền tệ và tích hợp nhiều plugin kinh tế. Plugin cung cấp hệ thống GUI trực quan, dễ quản lý, kèm theo hệ thống giao hàng và trình chỉnh sửa vật phẩm tiện lợi.

✨ Tính năng nổi bật

✅ Hỗ trợ đa tiền tệ:
> - Vault (tiền mặc định)
> - PlayerPoints
> - TokenManager
> - VNEconomy

✅ Tự động fallback nếu loại tiền chính không khả dụng.
✅ Shop GUI trực quan với khả năng chỉnh sửa item ngay trong game.
✅ Hệ thống giao hàng an toàn, tránh mất item khi full kho.
✅ Dễ dàng reload config mà không cần restart server.
✅ Tùy biến thông báo và giao diện.

⚙️ Cấu hình
> - Trong file config.yml, bạn có thể chỉnh loại tiền mặc định:

```currency:
  type: vault   # Có thể là: vault, playerpoints, tokenmanager, vneconomy
messages:
  shop-reloaded: "&aShop đã được reload thành công!"```

🔨 Lệnh
📌 Lệnh cho người chơi
> - /shop 👉 Mở giao diện Shop chính.

📌 Lệnh cho quản trị viên
> - /shopreload 👉 Reload lại config và dữ liệu shop.
✔ Quyền: **shop.admin**.

/shopadmin 👉 Mở menu quản lý shop, cho phép thêm/sửa/xóa danh mục và item.
✔ Quyền: **shop.admin**

🔑 Quyền hạn
> shop.admin
> ✔ Mặc định: OP
> ✔ Cho phép quản lý shop, chỉnh sửa và reload shop.