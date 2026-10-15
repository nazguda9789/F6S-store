"""Generate reviewed seed SQL from the isolated, imported source database."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB = 'f6s_merge_source'
CONTAINER = 'f6s-mysql-onboarding'

def command(args):
    return subprocess.check_output(['docker', 'exec', CONTAINER, *args]).decode('utf-8')

def query(sql):
    raw = command(['mysql', '--default-character-set=utf8mb4', '-uroot', '-N', '-B', '--raw', DB, '-e', sql])
    return [json.loads(line) for line in raw.splitlines() if line]

def dump(*args):
    return command(['mysqldump', '--default-character-set=utf8mb4', '-uroot',
                    '--set-gtid-purged=OFF', '--no-tablespaces', '--skip-comments',
                    '--skip-add-locks', '--skip-disable-keys', *args, DB])

tables = ['parent_category', 'category', 'color', 'size', 'product',
          'product_category', 'product_detail', 'product_image', 'product_color_image', 'blog']
schema = dump('--no-data', '--skip-add-drop-table')
schema = schema.replace('CREATE TABLE `', 'CREATE TABLE IF NOT EXISTS `')
schema = schema.replace('utf8mb4_0900_ai_ci', 'utf8mb4_unicode_ci')

def data(table, where=None):
    args = ['mysqldump', '--default-character-set=utf8mb4', '-uroot', '--set-gtid-purged=OFF',
            '--no-tablespaces', '--skip-comments', '--skip-add-locks', '--skip-disable-keys',
            '--no-create-info', '--insert-ignore', '--hex-blob']
    if where:
        args += ['--where=' + where]
    return command([*args, DB, table])

seed = '-- Additive seed: select f6s_shop first; no DROP DATABASE.\nSET NAMES utf8mb4;\n'
seed += schema
seed += '\nSET FOREIGN_KEY_CHECKS=0;\nSTART TRANSACTION;\n'
seed += data('users', "role IN ('ROLE_ADMIN','ROLE_EMPLOYEE')")
for table in tables:
    seed += data(table)
seed += '''
-- Map the same source data to the existing Vietnamese schema.
INSERT INTO danh_muc (ten_danh_muc)
SELECT DISTINCT c.name FROM category c
WHERE c.name IS NOT NULL AND NOT EXISTS
 (SELECT 1 FROM danh_muc d WHERE d.ten_danh_muc=c.name);

INSERT INTO tai_khoan (ten_dang_nhap,mat_khau,vai_tro,trang_thai)
SELECT u.email,u.password,IF(u.role='ROLE_ADMIN','ADMIN','NHAN_VIEN'),
 IF(u.actived=1 AND COALESCE(u.deleted,0)=0,1,0)
FROM users u WHERE u.role IN ('ROLE_ADMIN','ROLE_EMPLOYEE')
AND NOT EXISTS (SELECT 1 FROM tai_khoan t WHERE t.ten_dang_nhap=u.email);

INSERT INTO nhan_vien (ma_nhan_vien,ho_ten,email,trang_thai,tai_khoan_id)
SELECT CONCAT('SRCNV',u.id),COALESCE(u.full_name,u.email),u.email,t.trang_thai,t.id
FROM users u JOIN tai_khoan t ON t.ten_dang_nhap=u.email
WHERE u.role='ROLE_EMPLOYEE'
AND NOT EXISTS (SELECT 1 FROM nhan_vien n WHERE n.tai_khoan_id=t.id OR n.email=u.email);

INSERT INTO san_pham (ma_san_pham,ten_san_pham,thuong_hieu,mo_ta,gia_ban,hinh_anh,trang_thai,ngay_tao,danh_muc_id)
SELECT CONCAT('SRCSP',p.id),p.name,
 CASE WHEN UPPER(p.name) LIKE '%ADIDAS%' THEN 'Adidas'
 WHEN UPPER(p.name) LIKE '%NIKE%' THEN 'Nike'
 WHEN UPPER(p.name) LIKE '%JOMA%' THEN 'Joma'
 WHEN UPPER(p.name) LIKE '%MIZUNO%' THEN 'Mizuno'
 WHEN UPPER(p.name) LIKE '%ZOCKER%' THEN 'Zocker'
 WHEN UPPER(p.name) LIKE '%WIKA%' THEN 'Wika' ELSE NULL END,
 p.description,COALESCE(p.price,0),p.image,COALESCE(p.active,0),COALESCE(p.created_at,NOW()),
 COALESCE((SELECT MIN(d.id) FROM product_category pc JOIN category c ON c.id=pc.category_id
 JOIN danh_muc d ON d.ten_danh_muc=c.name WHERE pc.product_id=p.id),
 (SELECT id FROM danh_muc WHERE ten_danh_muc='Giày bóng đá' LIMIT 1))
FROM product p WHERE NOT EXISTS
 (SELECT 1 FROM san_pham s WHERE s.ma_san_pham=CONCAT('SRCSP',p.id) OR s.ten_san_pham=p.name);

INSERT INTO san_pham_chi_tiet (san_pham_id,kich_co,mau_sac,so_luong_ton)
SELECT sp.id,sz.name,c.name,SUM(COALESCE(pd.stock,0))
FROM product_detail pd JOIN product p ON p.id=pd.product_id
JOIN san_pham sp ON sp.ten_san_pham=p.name
JOIN size sz ON sz.id=pd.size_id JOIN color c ON c.id=pd.color_id
WHERE NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet x
 WHERE x.san_pham_id=sp.id AND x.kich_co=sz.name AND x.mau_sac=c.name)
GROUP BY sp.id,sz.name,c.name;

INSERT INTO bai_viet (tieu_de,noi_dung,hinh_anh,ngay_dang,tai_khoan_id)
SELECT b.title,COALESCE(b.content,''),b.image_banner,COALESCE(b.created_date,NOW()),
 (SELECT MIN(id) FROM tai_khoan WHERE vai_tro='ADMIN') FROM blog b
WHERE NOT EXISTS (SELECT 1 FROM bai_viet v WHERE v.tieu_de=b.title AND v.noi_dung=COALESCE(b.content,''));
COMMIT;
SET FOREIGN_KEY_CHECKS=1;
'''

seed = seed.replace('utf8mb4_0900_ai_ci', 'utf8mb4_unicode_ci')
(ROOT / 'sql').mkdir(exist_ok=True)
(ROOT / 'sql/bo_sung_du_lieu.sql').write_text(seed, encoding='utf-8')
base = (ROOT / 'f6s_shop.sql').read_text(encoding='utf-8')
marker = '-- BEGIN IMPORTED SOURCE SEED'
base = base.split(marker)[0].rstrip()
(ROOT / 'f6s_shop.sql').write_text(base + '\n\n' + marker + '\n' + seed, encoding='utf-8')

report = '# Danh sách dữ liệu bổ sung từ shopgiaythethao.sql\n\n'
report += 'Giữ nguyên dữ liệu mẫu cũ. Các bảng Java được bổ sung vì ứng dụng không đọc bảng tiếng Việt. '
report += 'Không sao chép khách hàng, đơn hàng, tin nhắn, OTP, phiên đăng nhập hoặc khóa dịch vụ. '
report += 'Mật khẩu tài khoản được giữ dưới dạng hash gốc, không biết mật khẩu rõ.\n\n'
report += '## Sản phẩm\n\n| ID nguồn | Tên | Giá (VND) |\n|---|---|---|\n'
for id_, name, price in query('SELECT JSON_ARRAY(id,name,price) FROM product ORDER BY id'):
    report += f'| {id_} | {name.strip()} | {price:,.0f} |\n'
report += '\n## Bài viết\n\n| ID nguồn | Tiêu đề |\n|---|---|\n'
for id_, title in query('SELECT JSON_ARRAY(id,title) FROM blog ORDER BY id'):
    report += f'| {id_} | {title.strip()} |\n'
report += '\n## Admin và nhân viên\n\n| ID nguồn | Họ tên | Email đăng nhập | Vai trò |\n|---|---|---|---|\n'
for row in query("SELECT JSON_ARRAY(id,full_name,email,role) FROM users WHERE role IN ('ROLE_ADMIN','ROLE_EMPLOYEE') ORDER BY id"):
    report += '| ' + ' | '.join(str(x) for x in row) + ' |\n'
report += '\n## Dữ liệu liên quan\n\n| Bảng | Số dòng nguồn |\n|---|---|\n'
for table in tables:
    count = query(f'SELECT JSON_ARRAY(COUNT(*)) FROM `{table}`')[0][0]
    report += f'| {table} | {count} |\n'
report += '\n## Cách import\n\nDatabase mới: import `f6s_shop.sql` (có DROP DATABASE, cần sao lưu). '
report += 'Database đang dùng: sao lưu, chọn database `f6s_shop` trong phpMyAdmin, '
report += 'import `sql/bo_sung_du_lieu.sql` để giữ dữ liệu hiện có. '
report += 'Bản bổ sung bỏ qua ID đã tồn tại trong bảng Java, không cập nhật/ghi đè dòng đó. '
report += 'Bảng tiếng Việt tránh trùng tên sản phẩm và email; bài viết so sánh tiêu đề và nội dung. '
report += 'Bài viết nguồn không có tác giả được gắn admin trong bảng tiếng Việt; bảng Java giữ NULL gốc.\n'
(ROOT / 'DANH_SACH_BO_SUNG.md').write_text(report, encoding='utf-8')
print('Generated full SQL, additive SQL, and detailed inventory.')
