# Schema trước và sau migration

Giá trị max_length là byte theo sys.columns. Hibernate validate sau migration sạch.

| Bảng | Cột | Trước | Sau |
|---|---|---|---|
| chat_lieu | id | Chưa có | int; max_length=4; nullable=False |
| chat_lieu | ten | Chưa có | nvarchar; max_length=200; nullable=False |
| chat_lieu | trang_thai | Chưa có | bit; max_length=1; nullable=False |
| customers | phone | varchar; max_length=20; nullable=True | nvarchar; max_length=40; nullable=True |
| customers | email | varchar; max_length=100; nullable=True | nvarchar; max_length=200; nullable=True |
| dia_chi_nhan_hang | id | Chưa có | int; max_length=4; nullable=False |
| dia_chi_nhan_hang | customer_id | Chưa có | int; max_length=4; nullable=False |
| dia_chi_nhan_hang | ten_nguoi_nhan | Chưa có | nvarchar; max_length=200; nullable=False |
| dia_chi_nhan_hang | so_dien_thoai | Chưa có | varchar; max_length=20; nullable=False |
| dia_chi_nhan_hang | dia_chi | Chưa có | nvarchar; max_length=510; nullable=False |
| dia_chi_nhan_hang | la_mac_dinh | Chưa có | bit; max_length=1; nullable=False |
| dia_chi_nhan_hang | thu_tu | Chưa có | tinyint; max_length=1; nullable=False |
| dia_chi_nhan_hang | created_at | Chưa có | datetime2; max_length=8; nullable=False |
| employees | phone | varchar; max_length=20; nullable=True | nvarchar; max_length=40; nullable=True |
| employees | email | varchar; max_length=100; nullable=True | nvarchar; max_length=200; nullable=True |
| order_details | size_name | varchar; max_length=50; nullable=True | nvarchar; max_length=100; nullable=True |
| order_details | ten_chat_lieu | Chưa có | nvarchar; max_length=200; nullable=True |
| orders | phone_number | varchar; max_length=20; nullable=True | nvarchar; max_length=40; nullable=True |
| orders | email | varchar; max_length=100; nullable=True | nvarchar; max_length=200; nullable=True |
| orders | da_tru_kho | Chưa có | bit; max_length=1; nullable=False |
| product_images | id | Chưa có | bigint; max_length=8; nullable=False |
| product_images | product_id | Chưa có | int; max_length=4; nullable=False |
| product_images | duong_dan | Chưa có | nvarchar; max_length=4096; nullable=False |
| product_images | thu_tu | Chưa có | int; max_length=4; nullable=False |
| product_images | la_anh_chinh | Chưa có | bit; max_length=1; nullable=False |
| product_variants | chat_lieu_id | Chưa có | int; max_length=4; nullable=False |
| products | image_url | varchar; max_length=500; nullable=True | nvarchar; max_length=1000; nullable=True |
| sizes | size_value | varchar; max_length=10; nullable=False | nvarchar; max_length=20; nullable=False |
