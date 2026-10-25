-- F6S SQL SERVER: only run against an EMPTY f6s_shop database.
-- Exported from current JPA entities with Hibernate SQLServerDialect.
-- No database/table is dropped; existing tables cause a stop.
USE f6s_shop;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped=0)
  THROW 51001, N'Database already contains tables. Skip 01 if backend created the schema; use 02 only after reviewing compatibility.', 1;
-- BEGIN HIBERNATE SQL SERVER DDL
create table dbo.categories (id bigint identity not null, active bit not null, name nvarchar(100) not null, primary key (id));
create table dbo.customer_addresses (id bigint identity not null, active bit not null, default_address bit not null, detail nvarchar(250) not null, district nvarchar(100) not null, phone nvarchar(15) not null, province nvarchar(100) not null, recipient_name nvarchar(120) not null, ward nvarchar(100) not null, customer_id bigint not null, primary key (id));
create table dbo.customers (id bigint identity not null, active bit not null, birth_date date, code nvarchar(30) not null, created_at datetimeoffset(6) not null, email nvarchar(254), full_name nvarchar(120) not null, gender nvarchar(10), phone nvarchar(15) not null, primary key (id));
create table dbo.email_challenges (id nvarchar(36) not null, attempts int not null, code_hash nvarchar(64) not null, consumed_at datetimeoffset(6), created_at datetimeoffset(6) not null, email nvarchar(254) not null, expires_at datetimeoffset(6) not null, grant_hash nvarchar(64), verified_at datetimeoffset(6), primary key (id));
create table dbo.media_assets (id nvarchar(36) not null, active bit not null, byte_size bigint not null, content_type nvarchar(20) not null, created_at datetimeoffset(6) not null, height int not null, original_name nvarchar(120) not null, sha256 nvarchar(64) not null, stored_name nvarchar(80) not null, uploaded_by nvarchar(100) not null, width int not null, primary key (id));
create table dbo.order_events (id bigint identity not null, actor nvarchar(100) not null, created_at datetimeoffset(6) not null, from_status nvarchar(30), note nvarchar(500) not null, to_status nvarchar(30) not null, order_id bigint not null, primary key (id));
create table dbo.order_items (id bigint identity not null, color nvarchar(60) not null, product_name nvarchar(180) not null, quantity int not null, size nvarchar(10) not null, sku nvarchar(60) not null, unit_price numeric(19,0) not null, order_id bigint not null, variant_id bigint not null, primary key (id));
create table dbo.order_refunds (id bigint identity not null, amount numeric(19,0) not null, created_at datetimeoffset(6) not null, reason nvarchar(500) not null, requested_by nvarchar(100) not null, resolved_at datetimeoffset(6), resolved_by nvarchar(100), restocked bit not null, settlement_reference nvarchar(120), status nvarchar(20) not null, order_id bigint not null, primary key (id));
create table dbo.orders (id bigint identity not null, address nvarchar(500) not null, carrier nvarchar(60), cash_change numeric(19,0), cash_received numeric(19,0), channel nvarchar(10) not null, code nvarchar(40) not null, completed_at datetimeoffset(6), created_at datetimeoffset(6) not null, customer_name nvarchar(120) not null, discount numeric(19,0) not null, email nvarchar(254) not null, note nvarchar(500) not null, payment_method nvarchar(10) not null, payment_status nvarchar(20), phone nvarchar(15) not null, promotion_code nvarchar(30), refunded_amount numeric(19,0), request_key nvarchar(36) not null, shipped_at datetimeoffset(6), shipping_fee numeric(19,0), status nvarchar(30) not null, subtotal numeric(19,0) not null, total numeric(19,0) not null, tracking_code nvarchar(100), customer_id bigint, shop_account_id nvarchar(36), primary key (id));
create table dbo.payment_attempts (id nvarchar(36) not null, amount numeric(19,0) not null, created_at datetimeoffset(6) not null, expires_at datetimeoffset(6) not null, provider_transaction nvarchar(40), settled_at datetimeoffset(6), status nvarchar(20) not null, order_id bigint not null, primary key (id));
create table dbo.pos_draft_invoices (invoice_id bigint not null, draft_id bigint not null, primary key (draft_id));
create table dbo.pos_drafts (id bigint identity not null, created_at datetimeoffset(6) not null, owner nvarchar(100) not null, request_key nvarchar(36) not null, status nvarchar(20) not null, customer_id bigint, primary key (id));
create table dbo.pos_lines (id bigint identity not null, quantity int not null, draft_id bigint not null, variant_id bigint not null, primary key (id));
create table dbo.pos_operators (username nvarchar(100) not null, primary key (username));
create table dbo.pos_scan_events (event_key nvarchar(36) not null, quantity int not null, sku nvarchar(60) not null, draft_id bigint not null, primary key (event_key));
create table dbo.product_color_images (id bigint identity not null, color nvarchar(60) not null, display_order int not null, media_id nvarchar(36) not null, product_id bigint not null, primary key (id));
create table dbo.product_variants (id bigint identity not null, active bit not null, color nvarchar(60) not null, price numeric(19,0) not null, shoe_size nvarchar(10) not null, sku nvarchar(60) not null, stock int not null, weight_grams int, product_id bigint not null, primary key (id), check (stock >= 0 and price > 0));
create table dbo.products (id bigint identity not null, active bit not null, brand nvarchar(100) not null, code nvarchar(40) not null, created_at datetimeoffset(6), created_by nvarchar(100), description nvarchar(4000) not null, featured bit not null, hover_image_url nvarchar(1000), image_url nvarchar(1000) not null, name nvarchar(180) not null, price numeric(19,0) not null, updated_at datetimeoffset(6), updated_by nvarchar(100), brand_attribute_id bigint, category_id bigint not null, material_attribute_id bigint, sole_attribute_id bigint, primary key (id), check (price > 0));
create table dbo.promotion_products (promotion_id bigint not null, product_id bigint not null, primary key (promotion_id, product_id));
create table dbo.promotions (id bigint identity not null, active bit not null, code nvarchar(30) not null, ends_at datetimeoffset(6) not null, max_discount numeric(19,0), max_uses int not null, min_subtotal numeric(19,0) not null, name nvarchar(120) not null, starts_at datetimeoffset(6) not null, type nvarchar(10) not null, used_count int not null, discount_value numeric(19,0) not null, primary key (id));
create table dbo.shoe_attributes (id bigint identity not null, active bit not null, code nvarchar(50) not null, name nvarchar(100) not null, attribute_type nvarchar(15) not null, primary key (id));
create table dbo.shop_accounts (id nvarchar(36) not null, active bit not null, address nvarchar(500), created_at datetimeoffset(6) not null, email nvarchar(254) not null, full_name nvarchar(120) not null, phone nvarchar(15), primary key (id));
create table dbo.shop_sessions (token_hash nvarchar(64) not null, expires_at datetimeoffset(6) not null, account_id nvarchar(36) not null, primary key (token_hash));
create table dbo.staff_accounts (id bigint identity not null, active bit not null, address nvarchar(500), birth_date date, code nvarchar(30) not null, created_at datetimeoffset(6) not null, email nvarchar(254) not null, full_name nvarchar(120) not null, gender nvarchar(10), password_hash nvarchar(100) not null, phone nvarchar(15) not null, username nvarchar(50) not null, primary key (id));
alter table dbo.categories add constraint UKt8o6pivur7nn124jehx7cygw5 unique (name);
alter table dbo.customers add constraint UKmkwx1x9mthieapj92cpxq5msc unique (code);
alter table dbo.customers add constraint UKm3iom37efaxd5eucmxjqqcbe9 unique (phone);
alter table dbo.media_assets add constraint UKabuiu4nkcx6ryvndd987bwdap unique (sha256);
alter table dbo.order_refunds add constraint UKawun3i7dq5g954aenqlv71lxq unique (order_id);
alter table dbo.orders add constraint UKgt3o4a5bqj59e9y6wakgk926t unique (code);
alter table dbo.orders add constraint UK7es2wsgsrff4jrc037l7aledf unique (request_key);
alter table dbo.payment_attempts add constraint UK4ujyoprdn2iwdfw154fitp87n unique (order_id);
alter table dbo.pos_draft_invoices add constraint UKhdt11u4v99dj0trggf27j8vh2 unique (invoice_id);
create index IDXo1r6ue98nn1geirom81gv5wbx on dbo.pos_drafts (owner, status);
alter table dbo.pos_drafts add constraint UKqkqmnc44fukh3bu2i32mg7efs unique (request_key);
alter table dbo.pos_lines add constraint UKmhmka4n44t9skp88xsl253cot unique (draft_id, variant_id);
alter table dbo.product_color_images add constraint UK9cw7nxj7rfyurrj8jkcrult5p unique (product_id, color, display_order);
alter table dbo.product_variants add constraint UK2qbxx2g9dwg33lulmrbfdbmdy unique (product_id, color, shoe_size);
alter table dbo.product_variants add constraint UKq935p2d1pbjm39n0063ghnfgn unique (sku);
alter table dbo.products add constraint UK57ivhy5aj3qfmdvl6vxdfjs4p unique (code);
alter table dbo.promotions add constraint UKjdho73ymbyu46p2hh562dk4kk unique (code);
alter table dbo.shoe_attributes add constraint UKg96g129re2nisac07t1l2g62j unique (attribute_type, name);
alter table dbo.shoe_attributes add constraint UKe29e05xxioqol9uem3d70ayfy unique (code);
alter table dbo.shop_accounts add constraint UK3bntq8fs8tbd9l4tx8tqhi0wf unique (email);
alter table dbo.staff_accounts add constraint UKdrcmypuiyfciyuna48awq8f7g unique (code);
alter table dbo.staff_accounts add constraint UK7fh415uxfcvf7hrv64hf8laq4 unique (email);
alter table dbo.staff_accounts add constraint UK6fb7onep8w4asmh3x7a8dufko unique (username);
alter table dbo.customer_addresses add constraint FKrvr6wl9gll7u98cda18smugp4 foreign key (customer_id) references dbo.customers;
alter table dbo.order_events add constraint FKjkqdh6opohx4rx9719gyn31aa foreign key (order_id) references dbo.orders;
alter table dbo.order_items add constraint FKbioxgbv59vetrxe0ejfubep1w foreign key (order_id) references dbo.orders;
alter table dbo.order_items add constraint FKemq71edpbn9wsxnxncfn1algp foreign key (variant_id) references dbo.product_variants;
alter table dbo.order_refunds add constraint FKmc53i8eq1lukxtbdulobibrek foreign key (order_id) references dbo.orders;
alter table dbo.orders add constraint FKpxtb8awmi0dk6smoh2vp1litg foreign key (customer_id) references dbo.customers;
alter table dbo.orders add constraint FKjlobcqigxkvvs64c335t4by3j foreign key (shop_account_id) references dbo.shop_accounts;
alter table dbo.payment_attempts add constraint FKkgs2arrxop1qax0kjc0a7gx7n foreign key (order_id) references dbo.orders;
alter table dbo.pos_draft_invoices add constraint FK2r5pnyrrnqjokhjof0329p36e foreign key (invoice_id) references dbo.orders;
alter table dbo.pos_draft_invoices add constraint FK57a9e70t1any2juqtwoc1trmq foreign key (draft_id) references dbo.pos_drafts;
alter table dbo.pos_drafts add constraint FKjmmcvvhmk40exbrkxunh0ckud foreign key (customer_id) references dbo.customers;
alter table dbo.pos_lines add constraint FKgycv641ef8ajnwjkk1eiyxped foreign key (draft_id) references dbo.pos_drafts;
alter table dbo.pos_lines add constraint FKtf6nfa90mtr6h09q56b3kxni5 foreign key (variant_id) references dbo.product_variants;
alter table dbo.pos_scan_events add constraint FK3brc8pen5dek4c30ronjtb2he foreign key (draft_id) references dbo.pos_drafts;
alter table dbo.product_color_images add constraint FKbf7q36wlbhh3dhh2fbl7qrme4 foreign key (media_id) references dbo.media_assets;
alter table dbo.product_color_images add constraint FK89befdlg42rvdht0jnon98wks foreign key (product_id) references dbo.products;
alter table dbo.product_variants add constraint FKosqitn4s405cynmhb87lkvuau foreign key (product_id) references dbo.products;
alter table dbo.products add constraint FKqio1svd4eqb8dp9owepvlkgrn foreign key (brand_attribute_id) references dbo.shoe_attributes;
alter table dbo.products add constraint FKog2rp4qthbtt2lfyhfo32lsw9 foreign key (category_id) references dbo.categories;
alter table dbo.products add constraint FKlfj0iy3v2x8fvgrlthe908pjh foreign key (material_attribute_id) references dbo.shoe_attributes;
alter table dbo.products add constraint FKe533eebgoetf4qi4c8cw65v62 foreign key (sole_attribute_id) references dbo.shoe_attributes;
alter table dbo.promotion_products add constraint FK9rm5m4rnoamh56kxetmoe1kk9 foreign key (product_id) references dbo.products;
alter table dbo.promotion_products add constraint FKkn7hllhf1o8jjrolro4rqmxt7 foreign key (promotion_id) references dbo.promotions;
alter table dbo.shop_sessions add constraint FKofv8b6n5wmqpomqd8di1tply foreign key (account_id) references dbo.shop_accounts;
-- END HIBERNATE SQL SERVER DDL
 COMMIT TRANSACTION;
 PRINT N'Created 24 application tables. Next run 02_real_products.sql.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
