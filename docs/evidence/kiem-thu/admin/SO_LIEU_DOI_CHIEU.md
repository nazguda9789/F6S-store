# SELECT độc lập và số hiển thị cùng thời điểm

Dữ liệu của nhóm POS/thống kê trước khi restore; SQL đầy đủ ở statistics-independent-sql.json.

| Kênh | Doanh thu SQL / trang | Số đơn SQL / trang | Trung bình SQL / trang |
|---|---:|---:|---:|
| ALL | 26510000 / 26510000 | 14 / 14 | 1893571.43 / 1893571.43 |
| ONLINE | 4650000 / 4650000 | 3 / 3 | 1550000 / 1550000 |
| POS | 21860000 / 21860000 | 11 / 11 | 1987272.73 / 1987272.73 |

## ALL — topQuantity (tối đa 10)

| Sản phẩm | SL SQL / trang | Doanh thu SQL / trang |
|---|---:|---:|
| Nike Air Max | 5 / 5 | 10000000 / 10.000.000 |
| Giày kiểm thử P12-0850 | 4 / 4 | 640000 / 640.000 |
| Giày kiểm thử P12-0900 | 4 / 4 | 640000 / 640.000 |
| QA Giày Đà Nẵng 00 | 4 / 4 | 400000 / 400.000 |
| Nike Dunk | 3 / 3 | 9000000 / 9.000.000 |
| Adidas Ultraboost | 2 / 2 | 5950000 / 5.950.000 |

## ALL — topRevenue (tối đa 10)

| Sản phẩm | SL SQL / trang | Doanh thu SQL / trang |
|---|---:|---:|
| Nike Air Max | 5 / 5 | 10000000 / 10.000.000 |
| Nike Dunk | 3 / 3 | 9000000 / 9.000.000 |
| Adidas Ultraboost | 2 / 2 | 5950000 / 5.950.000 |
| Giày kiểm thử P12-0850 | 4 / 4 | 640000 / 640.000 |
| Giày kiểm thử P12-0900 | 4 / 4 | 640000 / 640.000 |
| QA Giày Đà Nẵng 00 | 4 / 4 | 400000 / 400.000 |

## ONLINE — topQuantity (tối đa 10)

| Sản phẩm | SL SQL / trang | Doanh thu SQL / trang |
|---|---:|---:|
| Nike Air Max | 2 / 2 | 4000000 / 4.000.000 |
| Giày kiểm thử P12-0850 | 2 / 2 | 320000 / 320.000 |
| Giày kiểm thử P12-0900 | 2 / 2 | 320000 / 320.000 |

## ONLINE — topRevenue (tối đa 10)

| Sản phẩm | SL SQL / trang | Doanh thu SQL / trang |
|---|---:|---:|
| Nike Air Max | 2 / 2 | 4000000 / 4.000.000 |
| Giày kiểm thử P12-0850 | 2 / 2 | 320000 / 320.000 |
| Giày kiểm thử P12-0900 | 2 / 2 | 320000 / 320.000 |

## POS — topQuantity (tối đa 10)

| Sản phẩm | SL SQL / trang | Doanh thu SQL / trang |
|---|---:|---:|
| QA Giày Đà Nẵng 00 | 4 / 4 | 400000 / 400.000 |
| Nike Dunk | 3 / 3 | 9000000 / 9.000.000 |
| Nike Air Max | 3 / 3 | 6000000 / 6.000.000 |
| Adidas Ultraboost | 2 / 2 | 5950000 / 5.950.000 |
| Giày kiểm thử P12-0850 | 2 / 2 | 320000 / 320.000 |
| Giày kiểm thử P12-0900 | 2 / 2 | 320000 / 320.000 |

## POS — topRevenue (tối đa 10)

| Sản phẩm | SL SQL / trang | Doanh thu SQL / trang |
|---|---:|---:|
| Nike Dunk | 3 / 3 | 9000000 / 9.000.000 |
| Nike Air Max | 3 / 3 | 6000000 / 6.000.000 |
| Adidas Ultraboost | 2 / 2 | 5950000 / 5.950.000 |
| QA Giày Đà Nẵng 00 | 4 / 4 | 400000 / 400.000 |
| Giày kiểm thử P12-0850 | 2 / 2 | 320000 / 320.000 |
| Giày kiểm thử P12-0900 | 2 / 2 | 320000 / 320.000 |
