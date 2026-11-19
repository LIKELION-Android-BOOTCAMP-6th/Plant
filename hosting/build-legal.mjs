// docs/legal/*.md → hosting/public/*.html 변환 (#607)
// - 원본은 레포의 md 파일이고, 웹 페이지는 배포 직전에 이 스크립트로 만든다 (firebase.json hosting.predeploy)
// - 외부 패키지 없이 법률 문서에 쓰는 문법만 처리한다: 제목(#, ##, ###), 문단, 목록(-, 1.), 표, **굵게**, [링크](주소)
// - 처리하지 않는 문법이 있으면 조용히 깨지지 않도록 오류로 멈춘다
// 실행: node hosting/build-legal.mjs

import {readFileSync, writeFileSync} from "node:fs";
import {dirname, join} from "node:path";
import {fileURLToPath} from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");

// 앱(MyPageViewModel)이 여는 주소와 파일 이름이 같아야 한다
const PAGES = [
  {source: "docs/legal/privacy.md", output: "hosting/public/privacy-policy.html", title: "Plant - 개인정보처리방침"},
  {source: "docs/legal/terms.md", output: "hosting/public/terms.html", title: "Plant - 서비스 이용약관"},
];

// 운영 중이던 privacy-policy.html(2026-10-20 시행본)의 스타일을 그대로 쓴다
const STYLE = `
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Noto Sans KR', sans-serif;
            line-height: 1.8;
            color: #333;
            background: #fafafa;
            padding: 24px 16px;
        }
        .container {
            max-width: 720px;
            margin: 0 auto;
            background: #fff;
            padding: 40px 32px;
            border-radius: 12px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
        }
        h1 {
            font-size: 24px;
            margin-bottom: 8px;
            color: #1a1a1a;
        }
        .date {
            font-size: 14px;
            color: #888;
            margin-bottom: 32px;
        }
        h2 {
            font-size: 18px;
            margin-top: 32px;
            margin-bottom: 12px;
            color: #1a1a1a;
            padding-bottom: 8px;
            border-bottom: 1px solid #eee;
        }
        h3 {
            font-size: 15px;
            margin-top: 20px;
            margin-bottom: 8px;
            color: #444;
        }
        p, li {
            font-size: 15px;
            color: #555;
        }
        ul, ol {
            padding-left: 20px;
            margin-bottom: 12px;
        }
        li { margin-bottom: 4px; }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 12px 0;
            font-size: 14px;
        }
        th, td {
            border: 1px solid #e0e0e0;
            padding: 10px 12px;
            text-align: left;
        }
        th {
            background: #f5f5f5;
            font-weight: 600;
            color: #333;
        }
        .contact {
            background: #f8f9fa;
            padding: 16px 20px;
            border-radius: 8px;
            margin-top: 12px;
        }
        .contact p {
            margin-bottom: 4px;
        }
        @media (max-width: 480px) {
            body { padding: 12px 8px; }
            .container { padding: 24px 16px; }
            h1 { font-size: 20px; }
            h2 { font-size: 16px; }
            table { font-size: 13px; }
            th, td { padding: 8px; }
        }`;

const escapeHtml = (text) =>
  text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");

/** 한 줄 안의 **굵게**, [링크](주소)를 변환한다. 외부 링크는 새 창으로 연다 (운영 페이지와 같은 방식) */
function inline(text) {
  const html = escapeHtml(text)
      .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
      .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_, label, url) =>
        url.startsWith("http") ?
          `<a href="${url}" target="_blank" rel="noopener">${label}</a>` :
          `<a href="${url}">${label}</a>`,
      );
  if (/[*[\]`_]/.test(html.replace(/<[^>]+>/g, "").replace(/&[a-z]+;/g, ""))) {
    throw new Error(`처리하지 않는 문법이 있습니다: ${text}`);
  }
  return html;
}

function render(markdown) {
  const lines = markdown.replace(/\r\n/g, "\n").split("\n");
  const out = [];
  let i = 0;
  let contactSection = false; // "연락처"·"문의처" 절의 목록은 운영 페이지처럼 상자로 보여 준다

  while (i < lines.length) {
    const line = lines[i];
    if (line.trim() === "") {
      i++;
      continue;
    }

    const heading = /^(#{1,3}) (.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      out.push(`<h${level}>${inline(heading[2])}</h${level}>`);
      if (level === 2) contactSection = /연락처|문의처/.test(heading[2]);
      i++;
      // 제목 바로 아래 "시행일: …" 문단은 날짜 스타일
      if (level === 1) {
        while (i < lines.length && lines[i].trim() === "") i++;
        if (i < lines.length && lines[i].startsWith("시행일")) {
          out.push(`<p class="date">${inline(lines[i])}</p>`);
          i++;
        }
      }
      continue;
    }

    if (line.startsWith("|")) {
      const rows = [];
      while (i < lines.length && lines[i].startsWith("|")) rows.push(lines[i++]);
      if (rows.length < 2 || !/^\|[\s:|-]+\|$/.test(rows[1])) {
        throw new Error(`표 형식이 아닙니다: ${rows[0]}`);
      }
      const cells = (row) => row.slice(1, -1).split("|").map((c) => inline(c.trim()));
      out.push("<table>");
      out.push(`<tr>${cells(rows[0]).map((c) => `<th>${c}</th>`).join("")}</tr>`);
      for (const row of rows.slice(2)) {
        out.push(`<tr>${cells(row).map((c) => `<td>${c}</td>`).join("")}</tr>`);
      }
      out.push("</table>");
      continue;
    }

    const bullet = /^- /;
    const ordered = /^\d+\. /;
    if (bullet.test(line) || ordered.test(line)) {
      const pattern = bullet.test(line) ? bullet : ordered;
      const items = [];
      while (i < lines.length && pattern.test(lines[i])) items.push(lines[i++].replace(pattern, ""));
      if (contactSection && pattern === bullet) {
        out.push(`<div class="contact">${items.map((t) => `<p>${inline(t)}</p>`).join("")}</div>`);
      } else {
        const tag = pattern === bullet ? "ul" : "ol";
        out.push(`<${tag}>${items.map((t) => `<li>${inline(t)}</li>`).join("")}</${tag}>`);
      }
      continue;
    }

    if (/^\s/.test(line) || /^[>`#*]/.test(line)) {
      throw new Error(`처리하지 않는 문법이 있습니다: ${line}`);
    }

    // 문단: 빈 줄 전까지 이어지는 줄
    // 이어지는 줄도 첫 줄과 같은 기준으로 검사한다 (들여쓴 하위 목록 등이 문단 글자로 합쳐지지 않게)
    const paragraph = [];
    while (i < lines.length && lines[i].trim() !== "" &&
        !/^(#{1,3} |\||- |\d+\. )/.test(lines[i])) {
      if (/^\s/.test(lines[i]) || /^[>`#*]/.test(lines[i])) {
        throw new Error(`처리하지 않는 문법이 있습니다: ${lines[i]}`);
      }
      paragraph.push(lines[i++]);
    }
    out.push(`<p>${inline(paragraph.join(" "))}</p>`);
  }
  return out.map((l) => `        ${l}`).join("\n");
}

for (const page of PAGES) {
  const markdown = readFileSync(join(ROOT, page.source), "utf8");
  const html = `<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(page.title)}</title>
    <style>${STYLE}
    </style>
</head>
<body>
    <div class="container">
${render(markdown)}
    </div>
</body>
</html>
`;
  writeFileSync(join(ROOT, page.output), html, "utf8");
  console.log(`${page.source} → ${page.output}`);
}
