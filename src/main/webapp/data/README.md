# Danh mục địa chỉ tham khảo

`addresses-legacy.json` là bản rút gọn dữ liệu 3 cấp từ https://github.com/daohoangson/dvhcvn/blob/master/data/dvhcvn.json, tải ngày 18/10/2026. Chỉ giữ tên tỉnh/thành phố, tên và mã phường/xã, tên quận/huyện; không sao chép mã thực thi của dự án nguồn. Giấy phép nguồn: GPL-3.0, xem `ADDRESS-LICENSE.txt`. Bản nguồn đầy đủ: liên kết ở trên.

Đây là danh mục địa chỉ cũ trước thay đổi hành chính tháng 07/2025, không phải cam kết dữ liệu hiện hành. Giao diện ghi rõ giới hạn này và cho phép nhập địa chỉ mới ngoài danh mục, không bắt buộc quận/huyện. Các mã trong tập tin chỉ dùng tra cứu giao diện, không gửi làm mã GHN. Tên địa chỉ được gửi về các trường `provinceName`, `wardName`, `districtName` hiện có.

Tập tin được phục vụ cùng ứng dụng để chọn địa chỉ không cần token GHN hoặc gọi API địa chỉ bên ngoài. Đổi tỉnh sẽ xóa phường và quận/huyện cũ; chọn phường sẽ tự điền quận/huyện tương ứng. Phường trùng tên được phân biệt bằng tên quận/huyện trong danh sách.
