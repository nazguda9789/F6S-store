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
create table dbo.order_items (id bigint identity not null, color nvarchar(60) not null, product_name nvarchar(180) not null, quantity int not null, size nvarchar(10) not null, sku nvarchar(60) not null, unit_price numeric(19,0) not null, order_id bigint not null, variant_id bigint not null, primary key (id));
create table dbo.orders (id bigint identity not null, address nvarchar(500) not null, cash_change numeric(19,0), cash_received numeric(19,0), channel nvarchar(10) not null, code nvarchar(40) not null, completed_at datetimeoffset(6), created_at datetimeoffset(6) not null, customer_name nvarchar(120) not null, discount numeric(19,0) not null, email nvarchar(254) not null, note nvarchar(500) not null, payment_method nvarchar(10) not null, phone nvarchar(15) not null, promotion_code nvarchar(30), request_key nvarchar(36) not null, status nvarchar(30) not null, subtotal numeric(19,0) not null, total numeric(19,0) not null, primary key (id));
create table dbo.pos_draft_invoices (invoice_id bigint not null, draft_id bigint not null, primary key (draft_id));
create table dbo.pos_drafts (id bigint identity not null, created_at datetimeoffset(6) not null, owner nvarchar(100) not null, request_key nvarchar(36) not null, status nvarchar(20) not null, primary key (id));
create table dbo.pos_lines (id bigint identity not null, quantity int not null, draft_id bigint not null, variant_id bigint not null, primary key (id));
create table dbo.pos_operators (username nvarchar(100) not null, primary key (username));
create table dbo.pos_scan_events (event_key nvarchar(36) not null, quantity int not null, sku nvarchar(60) not null, draft_id bigint not null, primary key (event_key));
create table dbo.product_variants (id bigint identity not null, active bit not null, color nvarchar(60) not null, price numeric(19,0) not null, shoe_size nvarchar(10) not null, sku nvarchar(60) not null, stock int not null, product_id bigint not null, primary key (id), check (stock >= 0 and price > 0));
create table dbo.products (id bigint identity not null, active bit not null, brand nvarchar(100) not null, code nvarchar(40) not null, description nvarchar(4000) not null, featured bit not null, hover_image_url nvarchar(1000), image_url nvarchar(1000) not null, name nvarchar(180) not null, price numeric(19,0) not null, category_id bigint not null, primary key (id), check (price > 0));
create table dbo.promotion_products (promotion_id bigint not null, product_id bigint not null, primary key (promotion_id, product_id));
create table dbo.promotions (id bigint identity not null, active bit not null, code nvarchar(30) not null, ends_at datetimeoffset(6) not null, max_discount numeric(19,0), max_uses int not null, min_subtotal numeric(19,0) not null, name nvarchar(120) not null, starts_at datetimeoffset(6) not null, type nvarchar(10) not null, used_count int not null, discount_value numeric(19,0) not null, primary key (id));
alter table dbo.categories add constraint UKt8o6pivur7nn124jehx7cygw5 unique (name);
alter table dbo.orders add constraint UKgt3o4a5bqj59e9y6wakgk926t unique (code);
alter table dbo.orders add constraint UK7es2wsgsrff4jrc037l7aledf unique (request_key);
alter table dbo.pos_draft_invoices add constraint UKhdt11u4v99dj0trggf27j8vh2 unique (invoice_id);
create index IDXo1r6ue98nn1geirom81gv5wbx on dbo.pos_drafts (owner, status);
alter table dbo.pos_drafts add constraint UKqkqmnc44fukh3bu2i32mg7efs unique (request_key);
alter table dbo.pos_lines add constraint UKmhmka4n44t9skp88xsl253cot unique (draft_id, variant_id);
alter table dbo.product_variants add constraint UK2qbxx2g9dwg33lulmrbfdbmdy unique (product_id, color, shoe_size);
alter table dbo.product_variants add constraint UKq935p2d1pbjm39n0063ghnfgn unique (sku);
alter table dbo.products add constraint UK57ivhy5aj3qfmdvl6vxdfjs4p unique (code);
alter table dbo.promotions add constraint UKjdho73ymbyu46p2hh562dk4kk unique (code);
alter table dbo.order_items add constraint FKbioxgbv59vetrxe0ejfubep1w foreign key (order_id) references dbo.orders;
alter table dbo.order_items add constraint FKemq71edpbn9wsxnxncfn1algp foreign key (variant_id) references dbo.product_variants;
alter table dbo.pos_draft_invoices add constraint FK2r5pnyrrnqjokhjof0329p36e foreign key (invoice_id) references dbo.orders;
alter table dbo.pos_draft_invoices add constraint FK57a9e70t1any2juqtwoc1trmq foreign key (draft_id) references dbo.pos_drafts;
alter table dbo.pos_lines add constraint FKgycv641ef8ajnwjkk1eiyxped foreign key (draft_id) references dbo.pos_drafts;
alter table dbo.pos_lines add constraint FKtf6nfa90mtr6h09q56b3kxni5 foreign key (variant_id) references dbo.product_variants;
alter table dbo.pos_scan_events add constraint FK3brc8pen5dek4c30ronjtb2he foreign key (draft_id) references dbo.pos_drafts;
alter table dbo.product_variants add constraint FKosqitn4s405cynmhb87lkvuau foreign key (product_id) references dbo.products;
alter table dbo.products add constraint FKog2rp4qthbtt2lfyhfo32lsw9 foreign key (category_id) references dbo.categories;
alter table dbo.promotion_products add constraint FK9rm5m4rnoamh56kxetmoe1kk9 foreign key (product_id) references dbo.products;
alter table dbo.promotion_products add constraint FKkn7hllhf1o8jjrolro4rqmxt7 foreign key (promotion_id) references dbo.promotions;
-- END HIBERNATE SQL SERVER DDL
 COMMIT TRANSACTION;
 PRINT N'Created 12 application tables. Next run 02_real_products.sql.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
