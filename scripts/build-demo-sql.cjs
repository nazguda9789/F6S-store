// Build a fresh, isolated demo installer. Does not connect to a database.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const destination = process.argv[2];
if (!destination) throw new Error('Usage: node scripts/build-demo-sql.cjs /tmp/f6s_shop_demo.sql');
const source = fs.readFileSync(path.join(root, 'f6s_shop.sql'), 'utf8');
const expected = ['DROP DATABASE IF EXISTS f6s_shop;', 'CREATE DATABASE f6s_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;', 'USE f6s_shop;'];
for (const statement of expected) {
  if (source.split(statement).length !== 2) throw new Error('Unexpected installer structure: ' + statement);
}
const demo = source
  .replace(expected[0], '-- No DROP DATABASE: preserve existing databases. Installer requires a new f6s_shop_demo database.')
  .replace(expected[1], 'CREATE DATABASE f6s_shop_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
  .replace(expected[2], 'USE f6s_shop_demo;');
const business = fs.readFileSync(path.join(root, 'sql/du_lieu_mau_toan_bo_20261024.sql'), 'utf8');
fs.writeFileSync(destination, '-- DEMO ONLY. Creates f6s_shop_demo; does NOT replace f6s_shop. Stop import if that demo database already exists.\n' + demo + '\n' + business);
console.log('Built isolated demo installer: ' + destination);
