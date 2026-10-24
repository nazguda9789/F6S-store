// Build a fresh, isolated demo installer. Does not connect to a database.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const destination = process.argv[2];
if (!destination) throw new Error('Usage: node scripts/build-demo-sql.cjs /tmp/f6s_shop_demo.sql');
const source = fs.readFileSync(path.join(root, 'f6s_shop.sql'), 'utf8');
const expected = ['CREATE DATABASE f6s_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;', 'USE f6s_shop;'];
for (const statement of expected) {
  if (source.split(statement).length !== 2) throw new Error('Unexpected installer structure: ' + statement);
}
const demo = source
  .replace('Creates a NEW f6s_shop database', 'Creates a NEW f6s_shop_demo database')
  .replace(expected[0], 'CREATE DATABASE f6s_shop_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
  .replace(expected[1], 'USE f6s_shop_demo;');
const business = fs.readFileSync(path.join(root, 'sql/du_lieu_mau_sports_nghiep_vu_20261024.sql'), 'utf8');
fs.writeFileSync(destination, '-- F6S SPORTS ALL-IN-ONE — schema + new catalog + business demo.\n'
  + '-- Import THIS FILE ONLY into MySQL 8. No other SQL or old seed is required for a fresh demo.\n'
  + '-- Creates f6s_shop_demo; NO DROP DATABASE and does NOT replace your existing store.\n'
  + '-- If f6s_shop_demo already exists, STOP. Do not continue/ignore errors or use mysql --force.\n'
  + '-- Synthetic invoices affect demo reports. No OTP, service key or VNPAY receipt is fabricated.\n'
  + '-- Local admin: admin@gmail.com / 123123. Change before any public deployment.\n'
  + demo + '\n' + business);
console.log('Built isolated demo installer: ' + destination);
