# Old Continents Slider — Minecraft 1.21.1 NeoForge

[Old Continents Slider](https://www.curseforge.com/minecraft/mc-mods/old-continents)の大陸性スライダーを非公式にMinecraft 1.21.1 / NeoForgeに移植したプロジェクトです。

## ビルド

自分でModをビルドしたい場合、以下の環境が必要です。

- JDK 21
- NeoForge 21.1.252か、これより新しいバージョン

リポジトリをクローンして、同梱のGradle Wrapperを実行してください。

```powershell
.\gradlew.bat build
```

## 使い方

新規ワールド作成画面でワールドタイプ `Continents` を選び、必要ならカスタマイズ画面でスライダーを調整します。デフォルト値は `100` です。

| 値          | 動作                              |
| ---------- | ------------------------------- |
| `0`        | Minecraft 1.21.1の標準のOverworld生成 |
| `5` ～ `95` | 5 刻みで、海洋側への偏りと大陸・海洋の空間スケールを調整   |
| `100`      | 1.6.4時代風の大きな海洋を生成               |

値は海の面積の割合ではありません。`75` を選んでも海が75%になるわけではなく、seedや観測範囲で割合は変わります。また、Minecraft 1.6.4の生成アルゴリズムを厳密に再実装しているものではありません。

変更対象は地上世界の生成設定です。ネザーとエンドは標準の設定を使用します。生成済みチャンクを再生成する機能はありません。

## ワールド生成の仕組み

`ContinentsScreen` が現在のバイオームソースを維持した `NoiseBasedChunkGenerator` を作り、地上世界の生成器を置き換えます。設定 `0` は `minecraft:overworld`、それ以外は `continents:continentalness_<値>` を使います。

スライダー値を `c`、`t = c / 100` とすると、大陸性ノイズの加工は次の式になります。

```text
bias     = -0.38 × t
xz_scale = 0.25 × (1 - 0.65 × t)
rug      = 0.05 × t
k        = 1 - 0.62 × t

raw = bias
    + shifted_noise(minecraft:continentalness, xz_scale)
    + rug × noise(minecraft:continentalness, xz_scale = 1.5)

C = -0.9 + k × (raw + 0.9)   (-10 ≤ raw < -0.9)
C = raw                      (それ以外)
```

負の偏りは海洋側の領域を増やす方向へ、低い周波数は大陸と海のまとまりを大きくする方向へ作用します。細かいノイズを加え、極端な負の値は `-0.9` に向けて圧縮します。`-0.9` は補正範囲の境界で、陸と海を直接判定する閾値ではありません。

加工した大陸性は、気候サンプラーによるバイオーム選択に加え、地形の `offset`・`factor`・`jaggedness` のスプラインへ入力されます。その結果は `depth`、`sloped_cheese`、最終密度、`initial_density_without_jaggedness` に伝わります。バイオーム分布と地形の両方を変更しますが、海面は全設定で Y=63 のままです。

## オリジナル版との違い

移植元のオリジナルは26.2向けのModです。この移植版では公式 Minecraft 1.21.1 の Overworld 生成設定とスプラインを基に、元 Mod と同じ大陸性の加工式を適用しました。

- 26.2 の `preliminary_surface_level`、`find_top_surface`、`invert` を使用せず、1.21.1 の `initial_density_without_jaggedness` を使用します。
- 洞窟、地表ルール、ブロック、バイオームは1.21.1の定義を使用します。1.21.1 に存在しない Pale Garden や硫黄洞窟の色定義は含めません。
- バージョン固有の生成データが異なるため、同じシードでも移植元と同一の地形にはなりません。
- リソース ID、スライダーの範囲と丸め、コマンドの構文は維持します。既存の26.2ワールドの変換は対象外です。

20 種類の生成設定と各 7 種類の密度関数を同梱しています。再作成には Python 3 と `tools/generate_worldgen.py` を使えます。通常の Gradle ビルドに Python は不要です。

```powershell
python tools/generate_worldgen.py
# 公式クライアントJARを手元で指定する場合
python tools/generate_worldgen.py --client-jar path/to/1.21.1.jar
```

スクリプトは Mojang の公式1.21.1クライアントJARを取得し、SHA-1を確認してから生成データを読み込みます。取得したJARは `build/` に保存されますが、配布用Mod JARには含まれません。

## バイオーム分布図

ゲーム管理者権限（レベル2以上）で、次のコマンドを使用できます。

```text
/continents map 0
/continents map 50 10000
/continents map 100 10000
/continents map settings continents:continentalness_100 10000
```

数値は5刻みに丸めます。半径は `1000` ～ `30000` ブロック、省略時は `10000` です。現在の地上世界のシードとバイオームソースを使い、原点を中心とする一辺 `2 × 半径` の範囲を `1024 × 1024` ピクセルの PNG にします。画像はサーバーディレクトリの `continents-maps/` に保存します。同時に実行できる描画は1件です。

Y=63 を quart 座標へ変換してバイオームを評価する分布図であり、チャンクや地表を生成して描画する地形図ではありません。コマンドによって現在のワールドの生成設定が変わることもありません。

## 検証

`runGameTestServer` は単一の GameTest で全21設定を読み込み、固定シードでのバイオーム評価と地形の高さ計算、大陸性の有限性、海面、加工式の係数と丸めを確認します。GameTest用クラスと空の構造物は開発時だけ使用し、配布JARから除外します。

クライアントの自動動作確認は `.\gradlew.bat runClient -PclientSmokeTest=true` で実行できます。開発用の新規ワールドを作り、スライダーの初期値、キャンセル、値の適用、画面の再表示、ネザー・エンドの維持、ワールド作成を確認して終了します。結果は `run/client-smoke-result.txt`、画面画像は `run/screenshots/` に保存します。この検証用クラスも配布JARに含めません。

GameTest は地図コマンドの標準・独自設定と不明ID、同時描画の制限、PNGのサイズ、生成器が置き換わらないことも検証します。
