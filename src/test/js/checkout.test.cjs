const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
function fixture(response) {
    const elements = {};
    for (const id of ['shipping-status','province','provinceName','district','districtName','ward','wardName','khoiluong','total-tam-tinh','phiship']) {
        elements[id] = {value: '12', hidden: true, textContent: '', dataset: {},
            selectedOptions: [{dataset: {name: 'Test'}}], replaceChildren() {}};
    }
    elements.khoiluong.value = '500';
    elements['total-tam-tinh'].dataset.provisional = '200000';
    const label = {textContent: '', dataset: {}};
    const buttons = [{disabled: false}, {disabled: false}];
    const context = vm.createContext({
        document: {getElementById: id => elements[id],
            querySelector: () => label, querySelectorAll: () => buttons},
        window: {}, fetch: async () => response, URLSearchParams, Intl,
        Option: function(name, id) { this.text = name; this.value = id; this.dataset = {}; }
    });
    vm.runInContext(fs.readFileSync('src/main/webapp/js/checkout.js', 'utf8'), context);
    return {context, elements, label, buttons};
}
test('empty district list blocks checkout without indexing an absent item', async () => {
    const f = fixture({ok: true, json: async () => ({code: 200, data: []})});
    await vm.runInContext('loadHuyen()', f.context);
    assert.match(f.elements['shipping-status'].textContent, /Chưa có quận/);
    assert.ok(f.buttons.every(b => b.disabled));
});
test('provider failure is visible and blocks both desktop and mobile submit', async () => {
    const f = fixture({ok: false});
    await vm.runInContext('calShipping()', f.context);
    assert.equal(f.elements['shipping-status'].hidden, false);
    assert.ok(f.buttons.every(b => b.disabled));
});
test('shipping fee updates displayed fee, submitted fee and totals', async () => {
    const f = fixture({ok: true, json: async () => ({code: 200, data: {total: 35000}})});
    let recalculated = false;
    f.context.calTotalPrice = () => recalculated = true;
    await vm.runInContext('calShipping()', f.context);
    assert.equal(f.elements.phiship.value, 35000);
    assert.equal(f.label.dataset.shipcost, 35000);
    assert.ok(recalculated);
    assert.ok(f.buttons.every(b => !b.disabled));
});
test('free-shipping threshold keeps the submitted fee at zero', async () => {
    const f = fixture({ok: true, json: async () => ({code: 200, data: {total: 35000}})});
    f.elements['total-tam-tinh'].dataset.provisional = '500000';
    await vm.runInContext('calShipping()', f.context);
    assert.equal(f.elements.phiship.value, 0);
});
