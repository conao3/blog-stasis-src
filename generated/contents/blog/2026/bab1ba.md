Title: AIの「ソフトウェア工場」はいまどこまで来たのか
Date: 2026-11-21 14:20
Draft: true
Tags: ai-agent history vercel-board


コーディングエージェントを何体も並べて、人がほとんど見ないままPRがマージされていく開発を「ソフトウェア工場」と呼ぶ人が増えました。

<div class="tweet-card"><a href="https://x.com/happy_ryo/status/2123942567746375930"><span class="tweet-card-header"><span class="tweet-card-author"><strong>happy_ryo</strong><span>@happy_ryo</span></span><svg class="tweet-card-logo" viewBox="0 0 24 24" aria-hidden="true"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg></span><span class="tweet-card-text">Orcaとかを使わずに、自前でコーディングエージェントを用いたソフトウェアファクトリーを実装していて、こんな感じの作りにしている。一番気を掛けているのは、窓口になるエージェント1体以外は人間が見なくて良いこと。OSSとして公開してる。</span><time class="tweet-card-time" datetime="2026-11-20T19:52:23-0500">2026-11-20 19:52</time></a></div>

このポストを見て自分でも組みたくなり、この形がどう出来てきて、いまどこまで動いているのかを調べてみました。

調べてみると、多くの事例がよく似た構成に落ち着いていました。
事例の報告では人の仕事がコードを書くことから仕様と検査を用意することへ移っていたのですが、成果の数字は当事者が出したものばかりで外から確かめた結果はまだほとんどありません。

記事の後半では、同じ「ソフトウェア工場」という言葉が1968年にも使われていたという別の文脈の話も紹介します。


# 2023年から2025年、1体から並列へ

役割を分けたエージェントに開発をさせる発想は、LLMが出てすぐの2023年からありました。
[MetaGPT](https://arxiv.org/abs/2308.00352)の論文は、役割を持つエージェントに標準の手順を与える構成を「assembly line paradigm」と呼びました。

ところが同じ年に出たベンチマークの[SWE-bench](https://arxiv.org/abs/2310.06770)では、実際のGitHubのissueを解けた割合が最も良いモデルでも1.96%でした。
2024年3月にCognitionが出した[Devin](https://cognition.com/blog/introducing-devin)は、データセットの25%を無作為に選んだ部分でこの値を13.86%にしたと発表しています。

流れが変わったのは2025年5月で、Cursorの[background agent](https://cursor.com/changelog/0-50)とOpenAIの[Codex](https://openai.com/index/introducing-codex/)、GitHub Copilotの[coding agent](https://github.blog/news-insights/product-news/github-copilot-meet-the-new-coding-agent/)、Googleの[Jules](https://blog.google/technology/google-labs/jules/)の公開ベータが1週間のうちに出そろいました。
どれもタスクを渡すと離れた環境で作業してPRや差分を返すもので、Codexの発表は「work on many tasks in parallel」と書いています。

1人が何体も同時に動かす使い方は、ここで製品として使えるようになりました。

その2か月後にGeoffrey Huntleyが書いた[Ralph](https://ghuntley.com/ralph/)は、1体のエージェントを止めずに回し続ける使い方を最も短い形にしたものです。

```bash
while :; do cat PROMPT.md | claude-code ; done
```

同じプロンプトを繰り返し読ませて1回に1つの仕事をさせ、テストやビルドの失敗をエージェントに返して直させます。

同じ月にMETRが出した[実験](https://metr.org/blog/2025-07-10-early-2025-ai-experienced-os-dev-study/)では、経験のあるOSSの開発者がAIを使うと作業が19%長くかかり、本人たちは20%速くなったと感じていました。
速くなった実感と測った結果が食い違ったことは、あとで出てくる数字を読むときの前提になります。


# 人が話す場所と仕事を積む場所

OpenAIはのちにSymphonyの記事で、エージェントは速いのに人の注意が足りなくなったと書いています。
この問題に答えた道具を、私は人がどこで話すかを決めたものと、仕事をどこに積むかを決めたものの2つに分けて考えています。

人がどこで話すかを決めた道具として挙げたいのが、2025年11月にリポジトリが作られた[OpenClaw](https://github.com/openclaw/openclaw)です。
用途をコーディングに限らない個人のアシスタントで、自分のマシンで動かし続けてDiscordやSlackのような普段のチャットから話しかけます。

OpenClawではGatewayという常駐のプロセスがセッションと道具とチャットの接続を受け持ちます。
[スケジューラ](https://docs.openclaw.ai/automation/cron-jobs)が決まった時刻にエージェントを起こし、[sub-agent](https://docs.openclaw.ai/tools/subagents)は裏で動いて結果を頼んだ側へ知らせます。

人が話す相手は1体で、起こす役と裏の作業はその後ろに隠れます。
冒頭のポストの「窓口」と同じ形がここにあります。

仕事をどこに積むかを決めた道具が、2026年3月にリポジトリが公開されたOpenAIの[Symphony](https://github.com/openai/symphony)です。
4月の[記事](https://openai.com/index/open-source-codex-orchestration-symphony/)によると、技術者は1人で3から5のセッションまでなら無理なく見られたのですが、それを超えると切り替えの負担で生産性が落ちたそうです。

> For every open task, guarantee that an agent is running in its own workspace.

SymphonyはLinearのissueを見張り、開いているタスクごとに作業場を1つ用意してエージェントを動かし続けます。
セッションを人が見張る代わりに、issueの状態が仕事の進み具合を表します。

Vercelが2026-11-18に出した[Vercel Board](https://vercel.com/blog/introducing-vercel-board)は、この2つを1つにまとめたものだと私は考えています。
Boardはエージェントと人が共有する掲示板で、スレッドは会話であると同時にタスクにもなり、タスクは期限付きで1体だけに貸し出されます。


# 2026年、工場と呼ばれはじめる

2026年に入ると、この形を「工場」と呼ぶ文章が出てきます。
Dan Shapiroは1月の[記事](https://www.danshapiro.com/blog/2026/01/the-five-levels-from-spicy-autocomplete-to-the-software-factory/)でAIの使い方を0から5までの段階に分けて、最後の段階を「Dark Factory」と呼びました。

仕様を入れるとソフトウェアが出てくる黒い箱のことで、名前はロボットだけが働くファナックの工場から取ったと書いています。
人が要らないので照明も要らない工場という、製造業の言葉です。

その段階を運用していると公表したのがStrongDMで、2月に出した[Software Factoryのページ](https://factory.strongdm.ai/)には次の2つの決まりが載っています。

> Code must not be written by humans
> 
> Code must not be reviewed by humans

Simon Willisonの[紹介](https://simonwillison.net/2026/Feb/7/software-factory/)によると、人が読まない代わりにエージェントには見せない検証用のシナリオを用意して、OktaやSlackのような外部サービスの模造品の上で動きを確かめるそうです。
費用については、技術者1人あたり1日に1,000ドルをトークンに使っていなければ改善の余地があるという目安が書かれています。

同じ時期には、工場とは名乗らずに構成を詳しく公開した報告も出ています。
Cursorは長時間の自律コーディングの[報告](https://cursor.com/blog/scaling-agents)で、全員が同じ立場でロックを取り合う構成を最初に試したものの、20体いても2、3体ぶんの速さしか出なかったと書いています。

階層をなくす構成も試してうまくいかず、最終的には計画役と作業役と判定役に分けて、この構成でブラウザを一から作らせました。
品質を見る統合役も置いたのですが、ボトルネックになったので外したそうです。

Anthropicの[Cコンパイラの実験](https://www.anthropic.com/engineering/building-c-compiler)は司令役を置かず、16体のClaudeがgitのリポジトリに置いたロックのファイルで仕事を分けました。
約2,000回のセッションと2万ドルのAPIの費用で、Linuxのカーネルをビルドできる10万行のコンパイラができています。

OpenAIの[harness engineering](https://openai.com/index/harness-engineering/)の記事は、3人の技術者がCodexだけで約1,500件のPRをマージした経験を書いたもので、合言葉は「Humans steer. Agents execute.」です。

人が層の依存の向きを決めて、Codexに書かせたlintと構造のテストで守らせています。
コードが増える速さに人のQAが追いつかなくなったとも書いています。


# いまの到達点

並べてみると、多くの事例に共通する部品があります。

![img](../../../../resources/public/img/bab1ba-parts.svg)

仕事の列と1件を1体だけに渡す排他、エージェントごとに分けた作業場と機械による検査です。
Symphonyのように、検査のあとに人の受け入れを残している事例もあります。

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="org-left" />

<col  class="org-left" />

<col  class="org-left" />

<col  class="org-left" />
</colgroup>
<thead>
<tr>
<th scope="col" class="org-left">事例</th>
<th scope="col" class="org-left">仕事の列</th>
<th scope="col" class="org-left">排他と作業場</th>
<th scope="col" class="org-left">区切りと確かめ方</th>
</tr>
</thead>
<tbody>
<tr>
<td class="org-left">Cursorの実験</td>
<td class="org-left">計画役が作るタスク</td>
<td class="org-left">作業役が1件ずつ受け持つ</td>
<td class="org-left">判定役が周期ごとに続けるかを決める</td>
</tr>

<tr>
<td class="org-left">AnthropicのCコンパイラ</td>
<td class="org-left">各エージェントが次に直す問題を選ぶ</td>
<td class="org-left">gitに置くロックのファイルとコンテナごとのclone</td>
<td class="org-left">テストスイートとGCCとの比較</td>
</tr>

<tr>
<td class="org-left">Symphony</td>
<td class="org-left">Linearのissue</td>
<td class="org-left">issueごとの作業場</td>
<td class="org-left">CIの結果を付けて人が受け入れる</td>
</tr>

<tr>
<td class="org-left">Vercel Board</td>
<td class="org-left">掲示板のタスク</td>
<td class="org-left">期限付きの貸し出し</td>
<td class="org-left">修正が終わると着手できるレビューのタスク</td>
</tr>
</tbody>
</table>

OpenAIとStrongDMとAnthropicの報告では、人の仕事はコードを書くことから仕様と検査と環境を作ることへ移っています。

Lauren Tanは[講演](https://ai.engineer/code/2026/talks/lauren-tan)で、エージェントが同じ失敗を繰り返すときは注意書きを増やすより型やlintやテストで防ぐよう勧めました。
講演では月に約11,000件のPRで差し戻しが約0.5%だったとも報告しています。

ただ、工場の事例の数字はどれも当事者が出したものです。
VercelはBoardの上に作り直したv0の工場がPRの50%超をマージしていると書いていますが、分母も期間も差し戻しの割合も載っていません。

METRは2026年2月の[追跡調査](https://metr.org/blog/2026-02-24-uplift-update/)で、新しい実験のデータは現在の効果を測る信号として信頼できないと書きました。
AIなしで作業することを嫌って参加を断る開発者が増えたので、速くなる度合いを低く見積もる偏りが入ったそうです。


# 1968年にもあった「ソフトウェア工場」

ここからは別の文脈の話です。

いまの「工場」は製造業の無人工場に由来する言葉で、StrongDMやVercelの文書に昔のソフトウェア開発の話は出てきません。
ところが「ソフトウェア工場」という言葉そのものは58年前からあって、Addy Osmaniも[Software Factories, Light and Dark](https://addyosmani.com/blog/software-factories/)でこの言葉は1968年のBob Bemerまでさかのぼると書いています。

いまの工場がそこから生まれたという話は見つからなかったのですが、同じ言葉で何が試されたのかが面白かったので紹介します。

1968年のNATOのソフトウェア工学会議の[報告書](https://web.archive.org/web/2016/http://homepages.cs.ncl.ac.uk/brian.randell/NATO/nato1968.PDF)は、General ElectricにいたBemerの作業文書を次のように引いています。

> By utilizing the machine-controlled production environment, or software factory. Program construction, checkout and usage are done entirely within this environment using the tools it contains. Ideally it should be impossible to produce programs exterior to this environment.

Bemerが工場と呼んだのは、プログラムの作成と検査をすべてその中で行わせる、機械が管理する環境です。

翌1969年には日立がソフトウェアの部門を神奈川工場から独立させました。
日立の[沿革](https://www.hitachi.co.jp/Div/soft/history.html)はこれを「世界で最初のソフトウェア専門事業所」と書いていて、東芝や日本電気や富士通も1970年代に同じ方向へ進みます。

アメリカではSystem Development Corporationが1975年ごろに約200人のソフトウェア工場を作ったのですが、数年で使われなくなりました。
MITのMichael Cusumanoが1991年に書いた[草稿](https://www.gregorystrachta.com/resources/Touchstones/swp-3268-23661042.pdf)によると、顧客ごとのプロジェクトの責任者は仕様を工場へ渡すより自分で作りたがり、道具もプロジェクトの間で使い回せなかったそうです。

成果が数字で残っている工場は東芝の府中です。
同じ草稿によると納めたコードのうち既存のものを再利用した行の割合が1979年の13%から1985年の48%に上がり、東芝は生産性と品質が上がった主な理由を再利用だとしています。

一方でFred Brooksは1986年の[No Silver Bullet](https://www.cs.unc.edu/techreports/86-020.pdf)で次のように書きました。

> There is no single development, in either technology or management technique, which by itself promises even one order of magnitude improvement in productivity, in reliability, in simplicity.

ソフトウェアの難しさは仕様と設計と検査にあるので、書き表す手間を減らしても桁は変わらないという指摘です。

当時の工場は人の組織と手順の話で、いまの工場はエージェントの話なので、同じ言葉でも作っているものは違います。
それでも並べてみると、似た形が3つ見つかりました。

Bemerの「この環境の外ではプログラムを作れないようにする」は、Symphonyがタスクごとに用意する作業場や、冒頭のポストの図にある「決まりに反する操作を実行前に止める」フックと発想が似ています。

東芝が成果の理由に挙げたのは速く書くことよりも再利用で、OpenAIの記事が力を入れていたのもlintと構造のテストでした。
どちらも書く量を減らす側と確かめる側に手をかけています。

Brooksが難しいとした仕様と検査は、エージェントが書く手間を大きく減らしたあとも人の側に残っていて、StrongDMのシナリオはその難しさに人の時間を使った結果に見えます。


# 手元で組んでみた形

私もVercel Boardを常時動かしているデスクトップに置いて、Tailscaleのネットワークの中でノートPCと共有してみました。
タスクにホストとエージェントの種類のラベルを付けると、そのホストの常駐がタスクを取ってエージェントに渡します。

設定は[nixos-configurationのagent-board.nix](https://github.com/conao3/nixos-configuration/blob/master/home-manager/features/agent-board.nix)に置いてあります。
自分で書いたのはタスクを取ってエージェントを起こす数十行のシェルだけで、列と排他と記録はBoardに任せました。

タスクのスレッドに返信すると同じエージェントが続きを進めるようにしたので、ブラウザから依頼と返事を書くだけで仕事が進みます。

使ってみると数分おきに方針を変えるような企画の段階は、エージェントと直接話すほうが速く進みました。
工場に向いているのは方針が固まって切り出せる仕事です。

工場に渡す前の仕様と受け取ったあとに確かめる仕組みを私が用意できるかどうかで、結果が変わります。
次は人待ちになったタスクを知らせる通知と、検査を先に用意してから仕事を切る進め方を試すつもりです。

