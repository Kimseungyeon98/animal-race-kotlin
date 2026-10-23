// 캔버스 보드(.dc.html)를 정적 HTML로 그린다: sc-for / sc-if / {{값}} / dc-import(Animal) / 스크립트 renderVals
// 사용: node dc_render.js <board.dc.html> <tick> <out.html> [json props]
const fs = require('fs');
const path = require('path');
// 캔버스 프로젝트 폴더 (Artifact read로 받은 project/ 폴더). DC_PROJECT 환경 변수로 지정
const PROJECT = process.env.DC_PROJECT || path.join(process.cwd(), 'project');
// 주아체·Fredoka (앱에 들어 있는 폰트)
const FONTS = process.env.DC_FONTS || path.resolve(__dirname, '../../app/src/main/res/font');

class DCLogic { constructor(props) { this.props = props || {}; this.state = {}; } setState(s) { Object.assign(this.state, s); } }

const cache = {};
function load(file) {
  if (cache[file]) return cache[file];
  const src = fs.readFileSync(path.join(PROJECT, file), 'utf8');
  let tpl = src.slice(src.indexOf('<x-dc>') + 6, src.indexOf('</x-dc>'));
  tpl = tpl.replace(/<helmet>[\s\S]*?<\/helmet>/, '');
  const helmet = (src.match(/<helmet>([\s\S]*?)<\/helmet>/) || [, ''])[1];
  const sm = src.match(/<script type="text\/x-dc" data-dc-script[^>]*>([\s\S]*?)<\/script>/);
  const C = new Function('DCLogic', sm[1] + '\nreturn Component;')(DCLogic);
  return (cache[file] = { tpl, helmet, C });
}

function lookup(scope, expr) {
  expr = expr.trim();
  if (expr === 'true') return true;
  if (expr === 'false') return false;
  if (/^-?\d+(\.\d+)?$/.test(expr)) return Number(expr);
  let v = scope;
  for (const k of expr.split('.')) { if (v == null) return undefined; v = v[k]; }
  return v;
}

// 같은 이름의 여는/닫는 태그 짝 찾기 (중첩 고려)
function matchBlock(s, start, tag) {
  const open = new RegExp('<' + tag + '[\\s>]', 'g'), close = '</' + tag + '>';
  let depth = 0, i = start;
  while (i < s.length) {
    open.lastIndex = i;
    const o = open.exec(s);
    const c = s.indexOf(close, i);
    if (o && o.index < c) { depth++; i = o.index + 1; }
    else { depth--; if (depth === 0) return c; i = c + 1; }
  }
  throw new Error('unclosed ' + tag);
}

function render(tpl, scope, tick) {
  let out = '', i = 0;
  while (true) {
    const f = tpl.indexOf('<sc-for', i), g = tpl.indexOf('<sc-if', i);
    const n = [f, g].filter(x => x >= 0);
    if (!n.length) { out += tpl.slice(i); break; }
    const at = Math.min(...n), tag = at === f ? 'sc-for' : 'sc-if';
    out += tpl.slice(i, at);
    const headEnd = tpl.indexOf('>', at);
    const head = tpl.slice(at, headEnd + 1);
    const end = matchBlock(tpl, at, tag);
    const inner = tpl.slice(headEnd + 1, end);
    if (tag === 'sc-for') {
      const list = lookup(scope, head.match(/list="\{\{(.*?)\}\}"/)[1]) || [];
      const as = head.match(/as="(\w+)"/)[1];
      list.forEach((item, idx) => { out += render(inner, Object.assign({}, scope, { [as]: item, index: idx }), tick); });
    } else {
      const v = lookup(scope, head.match(/value="\{\{(.*?)\}\}"/)[1]);
      if (v && v !== 'false' && v !== '0') out += render(inner, scope, tick);
    }
    i = end + ('</' + tag + '>').length;
  }
  out = out.replace(/\{\{(.*?)\}\}/g, (m, e) => { const v = lookup(scope, e); return v == null ? '' : String(v); });
  out = out.replace(/<dc-import([^>]*)><\/dc-import>/g, (m, attrs) => {
    const props = {};
    attrs.replace(/(\w[\w-]*)="([^"]*)"/g, (mm, k, v) => { props[k] = v; });
    return renderComp(props.name + '.dc.html', props, tick);
  });
  return out;
}

function renderComp(file, props, tick) {
  const { tpl, C } = load(file);
  const c = new C(props);
  c.state = c.state || {};
  c.state.tick = tick;
  const vals = c.renderVals();
  return render(tpl, vals, tick);
}

function page(file, tick, props) {
  const { helmet } = load(file);
  const body = renderComp(file, props || {}, tick);
  const style = (helmet.match(/<style>([\s\S]*?)<\/style>/) || [, ''])[1];
  return `<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{font-family:'Jua';src:url(file://${FONTS}/jua.ttf)}
@font-face{font-family:'Fredoka';src:url(file://${FONTS}/fredoka_bold.ttf);font-weight:600 700}
${style} body{margin:0}</style></head><body>${body}</body></html>`;
}

module.exports = { page, renderComp };
if (require.main === module) {
  const [file, tick, out, props] = process.argv.slice(2);
  fs.writeFileSync(out, page(file, Number(tick || 0), props ? JSON.parse(props) : {}));
}
