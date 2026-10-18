const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
async function fixture(fail = false) {
    const elements = {};
    for (const id of ['local-province','local-ward','local-district','local-ward-name','local-address-help']) {
        elements[id] = {value:'', options:[], listeners:{},
            add(option) { this.options.push(option); },
            replaceChildren(...options) { this.options = options; },
            get selectedOptions() { return this.options.filter(o=>o.value === this.value); }
        };
    }
    const jquery = el => ({
        on: (name, handler) => { el.listeners[name] = handler; },
        trigger() {}, select2: options => { el.config = options; }
    });
    jquery.fn = {select2:true};
    let ready;
    const context = vm.createContext({
        document: {getElementById:id=>elements[id], addEventListener:(event, handler)=>{ready=handler;}},
        window: {jQuery:jquery}, jQuery:jquery,
        Option: function(text,value) { this.text=text;this.value=value;this.dataset={}; },
        fetch: async()=>({ok:!fail,json:async()=>[
            {name:'Thành phố A',wards:[{code:'001',name:'Phường Trùng',district:'Quận Một'},{code:'002',name:'Phường Trùng',district:'Quận Hai'}]},
            {name:'Tỉnh B',wards:[]}
        ]})
    });
    vm.runInContext(fs.readFileSync('src/main/webapp/js/local-address.js','utf8'),context);
    await ready();
    return elements;
}
test('province selection filters wards; duplicate names retain distinct districts',async()=>{
    const e=await fixture();
    e['local-province'].value='Thành phố A';e['local-province'].listeners.change();
    const ward=e['local-ward'];
    assert.equal(ward.options.length,3);
    assert.notEqual(ward.options[1].text,ward.options[2].text);
    ward.value='002';ward.listeners.change();
    assert.equal(e['local-ward-name'].value,'Phường Trùng');
    assert.equal(e['local-district'].value,'Quận Hai');
    e['local-province'].value='Tỉnh B';e['local-province'].listeners.change();
    assert.equal(ward.value,'');assert.equal(e['local-ward-name'].value,'');assert.equal(e['local-district'].value,'');
});
test('new address is submitted as typed without a legacy district',async()=>{
    const e=await fixture();const ward=e['local-ward'];
    ward.value='Phường mới';ward.add({value:ward.value,dataset:{}});ward.listeners.change();
    assert.equal(e['local-ward-name'].value,'Phường mới');assert.equal(e['local-district'].value,'');
});
test('search handles accents and uppercase Đ, and manual input is length limited',async()=>{
    const e=await fixture();const config=e['local-province'].config;
    const item={text:'Thành phố Đà Nẵng'};
    assert.equal(config.matcher({term:'da nang'},item),item);
    assert.equal(config.matcher({term:'khong ton tai'},item),null);
    assert.equal(config.createTag({term:'  Phường mới  '}).id,'Phường mới');
    assert.equal(config.createTag({term:'x'.repeat(101)}),null);
});
test('failed lookup leaves manual tags available and explains failure',async()=>{
    const e=await fixture(true);
    assert.match(e['local-address-help'].textContent,/Không tải được/);
    assert.equal(e['local-province'].config.tags,true);
});
test('bundled directory has complete ward identities within each province',()=>{
    const data=JSON.parse(fs.readFileSync('src/main/webapp/data/addresses-legacy.json','utf8'));
    assert.equal(data.length,63);
    for(const province of data) {
        assert.ok(province.name);const ids=new Set();
        for(const ward of province.wards) {
            assert.ok(ward.code && ward.name && ward.district);
            assert.ok(!ids.has(ward.code));ids.add(ward.code);
        }
    }
});
