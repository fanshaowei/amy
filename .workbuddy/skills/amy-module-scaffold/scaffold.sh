#!/usr/bin/env bash
#
# amy-module-scaffold —— 基于 quick-start/amy-module-template 生成新功能模块。
#
# 职责：
#   1. 复制模板到 <聚合模块>/<artifactId>
#   2. 替换 7 个占位符，并按包名末段重命名 com/amy/template、mapper/template 目录
#   3. 把 <module> 写进聚合模块的 pom.xml <modules>
#   4. 生成 Nacos 配置 <appName>-dev.yml（Nacos 可达时可选发布）
#   5. 执行 mvn -pl <module> -am test 跑 POC，校验 CRUD/分页/逻辑删除/数据权限/Redis
#
# 用法：
#   bash scaffold.sh --aggregator amy-biz-modules --artifact amy-biz-demo \
#       --package com.amy.demo --class AmyBizDemoApplication --port 9410 \
#       --desc "演示模块" [--name amy-biz-demo] [--parent amy-biz-modules] \
#       [--table-prefix spas] [--publish]
#
set -euo pipefail

# ---------- 参数解析 ----------
AGG="amy-biz-modules"
ART=""
PKG=""
CLS=""
PORT="9401"
DESC=""
NAME=""
PARENT=""
TABLE_PREFIX=""
PUBLISH=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --aggregator) AGG="$2"; shift 2;;
    --artifact)   ART="$2"; shift 2;;
    --package)    PKG="$2"; shift 2;;
    --class)      CLS="$2"; shift 2;;
    --port)       PORT="$2"; shift 2;;
    --desc)       DESC="$2"; shift 2;;
    --name)       NAME="$2"; shift 2;;
    --parent)     PARENT="$2"; shift 2;;
    --table-prefix) TABLE_PREFIX="$2"; shift 2;;
    --publish)    PUBLISH=1; shift;;
    -h|--help)    sed -n '3,20p' "$0"; exit 0;;
    *) echo "未知参数: $1" >&2; exit 1;;
  esac
done

# ---------- 必填校验 ----------
if [[ -z "$ART" || -z "$PKG" || -z "$CLS" || -z "$DESC" ]]; then
  echo "错误：--artifact / --package / --class / --desc 均为必填。" >&2
  exit 1
fi
[[ -z "$NAME" ]] && NAME="$ART"
[[ -z "$PARENT" ]] && PARENT="$(basename "$AGG")"

# 包名必须是 com.amy.<...>
if [[ "$PKG" != com.amy.* ]]; then
  echo "错误：基础包名必须以 com.amy. 开头（当前：$PKG）。" >&2
  exit 1
fi

# 业务表前缀：未显式传 --table-prefix 时，从应用名推导首字母缩写。
# 规则：去掉 amy-biz- 前缀，按 "-" 分词，取每段首字母小写拼接。
#   amy-biz-sun-palace-art-space -> sun palace art space -> spas
#   单段（如 demo）-> demo
if [[ -z "$TABLE_PREFIX" ]]; then
  TABLE_PREFIX="$(echo "$ART" | sed -E 's/^amy-biz-//; s/^amy-//' \
    | awk -F- '{out=""; for(i=1;i<=NF;i++){out=out substr($i,1,1)}; print tolower(out)}')"
fi
echo "    业务表前缀（biz_<缩写>_<业务>）：biz_$TABLE_PREFIX"

# ---------- 路径 ----------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
TEMPLATE="$REPO_ROOT/quick-start/amy-module-template"
TARGET="$REPO_ROOT/$AGG/$ART"
AGG_POM="$REPO_ROOT/$AGG/pom.xml"

if [[ ! -d "$TEMPLATE" ]]; then
  echo "错误：模板不存在：$TEMPLATE" >&2
  exit 1
fi
if [[ -e "$TARGET" ]]; then
  echo "错误：目标已存在：$TARGET" >&2
  exit 1
fi
if [[ "$AGG" != "." && ! -f "$AGG_POM" ]]; then
  echo "错误：聚合模块 pom 不存在：$AGG_POM" >&2
  exit 1
fi

JAVADIR="$(echo "$PKG" | sed 's/\./\//g')"          # com.amy.demo -> com/amy/demo
MAPPERDIR="$(echo "$PKG" | sed 's/.*\.//')"          # 末段，用作 mapper 子目录

# ---------- 导出给 python 用（避免特殊字符转义问题） ----------
export SC_ART="$ART" SC_NAME="$NAME" SC_PKG="$PKG" SC_CLS="$CLS" \
       SC_PORT="$PORT" SC_DESC="$DESC" SC_PARENT="$PARENT" \
       SC_TABLE_PREFIX="$TABLE_PREFIX" \
       SC_TARGET="$TARGET" SC_AGG_POM="$AGG_POM" SC_AGG="$AGG"

echo "==> [1/5] 复制模板 $TEMPLATE -> $TARGET"
cp -R "$TEMPLATE" "$TARGET"

echo "==> [2/5] 替换占位符"
python3 - <<'PY'
import os, io
art=os.environ['SC_ART']; name=os.environ['SC_NAME']; pkg=os.environ['SC_PKG']
cls=os.environ['SC_CLS']; port=os.environ['SC_PORT']; desc=os.environ['SC_DESC']
parent=os.environ['SC_PARENT']; table_prefix=os.environ['SC_TABLE_PREFIX']
target=os.environ['SC_TARGET']; agg_pom=os.environ['SC_AGG_POM']

tokens = {
    "__APP_ARTIFACT__": art,
    "__APP_NAME__": name,
    "__BASE_PACKAGE__": pkg,
    "__APP_CLASS__": cls,
    "__APP_PORT__": port,
    "__APP_DESC__": desc,
    "__PARENT_ARTIFACT__": parent,
    "__TABLE_PREFIX__": "biz_" + table_prefix,
}
exts = (".java",".xml",".yml",".yaml",".properties",".md",".txt",".sql")
def walk_replace(root):
    for dp,_,fns in os.walk(root):
        if '/target/' in dp or dp.endswith('/target'):
            continue
        for fn in fns:
            if not fn.endswith(exts):
                continue
            p=os.path.join(dp,fn)
            try:
                with open(p,'r',encoding='utf-8') as f:
                    s=f.read()
            except UnicodeDecodeError:
                continue
            new=s
            for k,v in tokens.items():
                if k in new:
                    new=new.replace(k,v)
            if new!=s:
                with open(p,'w',encoding='utf-8') as f:
                    f.write(new)
walk_replace(target)

# 聚合模块 pom 插入 <module>
if agg_pom and os.path.exists(agg_pom) and agg_pom != os.path.join(target,'pom.xml'):
    with open(agg_pom,'r',encoding='utf-8') as f:
        pom=f.read()
    mod=f"        <module>{art}</module>\n"
    if f"<module>{art}</module>" not in pom:
        if "</modules>" in pom:
            pom=pom.replace("</modules>", mod+"    </modules>",1)
            with open(agg_pom,'w',encoding='utf-8') as f:
                f.write(pom)
            print(f"    已把 <module>{art}</module> 写入 {agg_pom}")
        else:
            print(f"    警告：{agg_pom} 无 <modules> 节点，跳过；请手动注册模块。")
PY

echo "==> [3/5] 重命名包/资源目录"
MAINJ="$TARGET/src/main/java"; TESTJ="$TARGET/src/test/java"; MAPR="$TARGET/src/main/resources/mapper"
mkdir -p "$(dirname "$MAINJ/$JAVADIR")" "$(dirname "$TESTJ/$JAVADIR")" "$(dirname "$MAPR/$MAPPERDIR")"
[[ -d "$MAINJ/com/amy/template" ]] && mv "$MAINJ/com/amy/template" "$MAINJ/$JAVADIR"
[[ -d "$TESTJ/com/amy/template" ]] && mv "$TESTJ/com/amy/template" "$TESTJ/$JAVADIR"
[[ -d "$MAPR/template" ]] && mv "$MAPR/template" "$MAPR/$MAPPERDIR"

# Nacos 配置文件重命名
NACOSDIR="$TARGET/src/main/resources/nacos"
if [[ -f "$NACOSDIR/nacos-dev.yml" ]]; then
  mv "$NACOSDIR/nacos-dev.yml" "$NACOSDIR/$NAME-dev.yml"
  echo "    已生成 Nacos 配置：$NACOSDIR/$NAME-dev.yml"
fi

# ---------- 可选：发布到 Nacos ----------
if [[ "$PUBLISH" -eq 1 ]]; then
  NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
  echo "==> [4/5] 发布 Nacos 配置到 $NACOS_ADDR (dataId=$NAME-dev.yml)"
  CFG="$NACOSDIR/$NAME-dev.yml"
  if curl -s -m 5 "http://$NACOS_ADDR/nacos/v1/ns/operator/metrics" >/dev/null 2>&1; then
    # 读取命名空间（默认空=public）；tenant 留空即可
    content="$(python3 -c "import sys,urllib.parse;print(urllib.parse.quote(open(sys.argv[1],encoding='utf-8').read()))" "$CFG")"
    http_code=$(curl -s -o /dev/null -w "%{http_code}" -m 10 -X POST \
      "http://$NACOS_ADDR/nacos/v1/cs/configs?dataId=$NAME-dev.yml&group=DEFAULT_GROUP&content=$content")
    echo "    Nacos publish HTTP=$http_code (200/20007=成功/已存在)"
  else
    echo "    警告：Nacos($NACOS_ADDR) 不可达，跳过发布；配置文件已生成本地，可稍后手动导入。"
  fi
else
  echo "==> [4/5] 跳过 Nacos 发布（未传 --publish）。配置已生成本地：$NACOSDIR/$NAME-dev.yml"
fi

# ---------- 跑 POC 测试 ----------
echo "==> [5/5] 执行 POC 测试：mvn -pl $AGG/$ART -am test"
set +e
mvn -q -pl "$AGG/$ART" -am test
RC=$?
set -e
if [[ $RC -eq 0 ]]; then
  echo "=================================================="
  echo " POC 测试通过 ✅  模块已生成：$TARGET"
  echo " 启动类：$PKG.$CLS"
  echo " 聚合模块：$AGG (pom 已注册 <module>$ART</module>)"
  echo " Nacos 配置：$NACOSDIR/$NAME-dev.yml"
  echo "=================================================="
else
  echo "=================================================="
  echo " POC 测试失败 ❌ (mvn exit=$RC)。请查看上方 Surefire 输出定位问题。"
  echo " 模块文件已生成，可手动修复后重跑："
  echo "   mvn -pl $AGG/$ART -am test -Dtest=MpPocDemoTest"
  echo "=================================================="
fi
exit $RC
