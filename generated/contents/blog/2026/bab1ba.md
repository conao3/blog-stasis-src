Title: AIの「ソフトウェア工場」はいまどこまで来たのか
Date: 2026-11-21 14:20
Tags: ai-agent history vercel-board


コーディングエージェントを何体も並べて動かし、人がほとんど見ないままPRがマージされる開発を「ソフトウェア工場」と呼ぶ人が増えました。

<div class="tweet-card"><a href="https://x.com/happy_ryo/status/2123942567746375930"><span class="tweet-card-header"><img class="tweet-card-avatar" src="/img/tweet-avatar-happy_ryo.jpg" alt=""><span class="tweet-card-author"><strong>いわまりょう</strong><span>@happy_ryo</span></span><svg class="tweet-card-logo" viewBox="0 0 24 24" aria-hidden="true"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg></span><span class="tweet-card-text">Orcaとかを使わずに、自前でコーディングエージェントを用いたソフトウェアファクトリーを実装していて、こんな感じの作りにしている。一番気を掛けているのは、窓口になるエージェント1体以外は人間が見なくて良いこと。OSSとして公開してる。</span><span class="tweet-card-media" data-count="3"><img src="/img/tweet-2123942567746375930-1.jpg" alt="ポストの画像" loading="lazy"><img src="/img/tweet-2123942567746375930-2.jpg" alt="ポストの画像" loading="lazy"><img src="/img/tweet-2123942567746375930-3.jpg" alt="ポストの画像" loading="lazy"></span><time class="tweet-card-time" datetime="2026-11-20T19:52:23-0500">2026-11-20 19:52</time></a></div>

このポストを見て自分でも組みたくなり、こうした仕組みがどうできてきたのかと、いまどこまで動いているのかを調べてみました。

多くの事例でよく似た構成が使われていました。
事例の報告では人の仕事がコードを書くことから仕様と検査を用意することへ移っていたのですが、成果の数字は当事者が出したものばかりで外から確かめた結果はまだほとんどありません。

記事の後半では別の文脈に移り、1968年にも「ソフトウェア工場」という言葉が使われていたことを紹介します。


# 2023年から2025年、1体から並列へ

役割を分けたエージェントに開発させる発想はLLMが出てすぐの2023年からあり、[MetaGPT](https://arxiv.org/abs/2308.00352)の論文では役割を持つエージェントに標準の手順を与える構成を「assembly line paradigm」と呼びました。

ところが同じ年に出たベンチマークの[SWE-bench](https://arxiv.org/abs/2310.06770)では、最も成績の良いモデルでも実際のGitHubのissueを解けた割合は1.96%でした。
Cognitionは2024年3月に[Devin](https://cognition.com/blog/introducing-devin)を発表し、データセットから無作為に選んだ25%でこの割合が13.86%に達したと報告しています。

2025年2月にはAnthropicが[Claude Code](https://www.anthropic.com/news/claude-3-7-sonnet)を研究プレビューとして出しました。
ターミナルから大きな作業をまるごと任せられる道具で、このあと出てくるRalphとAnthropicの実験はClaude Codeを、OpenAIの2つの記事はCodexを使っています。

2025年5月にはCursorの[background agent](https://cursor.com/changelog/0-50)とOpenAIの[Codex](https://openai.com/index/introducing-codex/)、GitHub Copilotの[coding agent](https://github.blog/news-insights/product-news/github-copilot-meet-the-new-coding-agent/)、Googleの[Jules](https://blog.google/technology/google-labs/jules/)の公開ベータが1週間のうちに出そろいました。
どれもタスクを渡すと離れた環境で作業してPRや差分を返すもので、Codexの発表には「work on many tasks in parallel」とあります。

この時期に製品として1人で何体ものエージェントを同時に動かせるようになりました。

その2か月後にGeoffrey Huntleyが書いた[Ralph](https://ghuntley.com/ralph/)は、1体のエージェントを止めずに回し続ける使い方を最も短い形で表したものです。

```bash
while :; do cat PROMPT.md | claude-code ; done
```

同じプロンプトを繰り返し読ませて1回に1つの仕事をさせ、テストやビルドの失敗をエージェントに返して直させます。
のちに[OpenAI](https://openai.com/index/harness-engineering/)と[Anthropic](https://www.anthropic.com/engineering/building-c-compiler)は、それぞれの記事で自分たちのループをRalphになぞらえています。

同じ月にMETRが出した[実験](https://metr.org/blog/2025-07-10-early-2025-ai-experienced-os-dev-study/)では、経験のあるOSSの開発者がAIを使うと作業時間が19%長くなり、本人たちは20%速くなったと感じていました。
速くなった実感と測った結果が食い違ったことは、あとで出てくる数字を読むときの前提になります。


# 2026年の1月と2月、2つの動き

2026年の1月と2月には、エージェントを使う開発を「工場」と呼ぶ文章と、工場とは名乗らずに構成と結果を詳しく報告する文章の2種類が出ました。


## 工場と呼ぶ文章

1月にSteve Yeggeが[Gas Town](https://yegge.ai/gastown)を公開しました。
Yeggeは2025年10月に仕事を記録する[Beads](https://github.com/gastownhall/beads)を出していて、それを使ってGas Townを作りました。

市長役が全体を調整し、見張り役が作業役の仕事を確認してマージ役が順にマージする構成で、Yeggeはのちにこれを「the first-ever software factory」と呼んでいます。

同じ1月にDan Shapiroは[記事](https://www.danshapiro.com/blog/2026/01/the-five-levels-from-spicy-autocomplete-to-the-software-factory/)でAIの使い方を0から5までに分け、仕様を入れるとソフトウェアが出てくる黒い箱を最後の「Dark Factory」と呼びました。
人が要らないので照明も要らない工場を指す製造業の言葉で、Shapiroはロボットだけが働くファナックの工場から名前を取ったと書いています。

2月にはStrongDMがその段階を運用していると公表して、[Software Factoryのページ](https://factory.strongdm.ai/)に次の2つの決まりを載せました。

> Code must not be written by humans
> 
> Code must not be reviewed by humans

Simon Willisonの[紹介](https://simonwillison.net/2026/Feb/7/software-factory/)によると、人がコードを読まない代わりにエージェントには見せない検証用のシナリオを用意し、OktaやSlackのような外部サービスの模造品を使って動きを確かめるそうです。
技術者1人あたり1日に1,000ドルをトークンに使っていなければ改善の余地があると、StrongDMは費用の目安を示しています。


## 構成と結果の報告

同じ2か月のあいだには工場という言葉を使わない報告も3本出ています。

1月にCursorが長時間の自律コーディングの[報告](https://cursor.com/blog/scaling-agents)を出しました。
最初は全員が同じ立場でロックを取り合う構成を試したものの、20体いても2、3体ぶんの速さしか出なかったと書いています。

階層をなくす構成もうまくいかず、最終的に計画役と作業役と判定役に分けてブラウザを一から作らせました。
品質を見る統合役も置いたのですが、そこで作業が滞ったので外したそうです。

2月にはAnthropicが[Cコンパイラの実験](https://www.anthropic.com/engineering/building-c-compiler)を公開しました。

司令役を置かずに16体のClaudeがgitのリポジトリに置いたロックファイルで仕事を分けました。
約2,000回のセッションを実行し、APIに2万ドルを使ってLinuxのカーネルをビルドできる10万行のコンパイラができています。

同じ2月にOpenAIが出した[harness engineering](https://openai.com/index/harness-engineering/)の記事では、3人の技術者がCodexだけで約1,500件のPRをマージした経験を報告していて、合言葉は「Humans steer. Agents execute.」です。
人が層どうしの依存関係の向きを決め、Codexが書いたlintと構造のテストでその向きを守らせています。

コードが増える速さに人のQAが追いつかなくなったとも書いています。


# 人が話す場所と仕事を積む場所

OpenAIは2026年4月に出した[Symphonyの記事](https://openai.com/index/open-source-codex-orchestration-symphony/)で、harness engineeringの次には人がセッションを見ていられる量に限界が来たと書いています。
技術者は1人で3から5のセッションまでなら無理なく見られたのですが、それを超えると切り替えの負担で生産性が落ちたそうです。

人が複数のセッションを見続ける負担を減らす道具は、工場という言葉が出てくる前の2025年から作られていました。
私はそれを仕事をどこに積むかを決めた道具と、人がどこで話すかを決めた道具の2つに分けて考えています。


## 仕事を積む場所

2025年6月に[Vibe Kanban](https://github.com/BloopAI/vibe-kanban)が、10月に先ほどのBeadsが出ています。
Vibe Kanbanはカンバンのissueで仕事を計画してエージェントに渡す道具で、Beadsにはエージェントの仕事を記録します。

2026年3月にOpenAIがリポジトリを公開した[Symphony](https://github.com/openai/symphony)によって、仕事をissueで管理する仕組みが広く知られるようになったと私は見ています。
OpenAIは自社の運用を仕様ごと公開し、一部の人が使っていた仕組みを誰でも試せるようになりました。

> For every open task, guarantee that an agent is running in its own workspace.

SymphonyはLinearのissueを監視し、開いているタスクごとに作業場を1つ用意してエージェントを動かし続けます。
セッションを人が見張る代わりに、issueの状態が仕事の進み具合を表します。


## 人が話す場所

2025年11月にリポジトリが作られた[OpenClaw](https://github.com/openclaw/openclaw)は用途をコーディングに限らない個人のアシスタントです。
自分のマシンで動かし続けてDiscordやSlackのような普段のチャットから話しかけます。

この記事を書いている時点でGitHubのstarは39万を超えていて、Claude Codeのリポジトリの2倍以上あります。

OpenClawではGatewayという常駐プロセスがチャットとセッションと道具をつなぎます。
決まった時刻になると[スケジューラ](https://docs.openclaw.ai/automation/cron-jobs)がエージェントを起こし、[sub-agent](https://docs.openclaw.ai/tools/subagents)は裏で動いて結果を頼んだ側に返します。

裏でエージェントを起こして仕事を進めても、人が話す相手は冒頭のポストの「窓口」と同じく1体です。


## 2つを1つにまとめる

Vercelが2026年11月に出した[Vercel Board](https://vercel.com/blog/introducing-vercel-board)は、仕事を積む場所と人が話す場所の2つを1つにまとめたものだと私は考えています。
Boardはエージェントと人が共有する掲示板で、スレッドは会話であると同時にタスクにもなり、タスクは期限付きで1体だけに貸し出されます。


# いまの到達点

多くの事例では仕事を列に並べて1件を1体にだけ割り当て、エージェントごとに作業場を分けて機械で成果を検査します。

![img](../../../../resources/public/img/bab1ba-parts.svg)

Symphonyのように検査を終えた仕事を人が受け入れる事例もあります。

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

OpenAIとStrongDMとAnthropicの報告では、コードを書く仕事がエージェントに移り、人は仕様と検査と環境を用意しています。

Lauren Tanは[講演](https://ai.engineer/code/2026/talks/lauren-tan)で、エージェントが同じ失敗を繰り返すなら注意書きを増やすより型やlintやテストで防ぐよう勧めました。
月に約11,000件のPRで差し戻しが約0.5%だったとも報告しています。

ただ工場の成果を示す数字はどれも当事者が公表したものです。
VercelはBoardで作り直したv0の工場がPRの50%超をマージしていると公表していますが、分母と集計期間と差し戻しの割合は示していません。

METRは2026年2月の[追跡調査](https://metr.org/blog/2026-02-24-uplift-update/)で、新しい実験のデータからAIの現在の効果を信頼できる形では測れないと書きました。
AIなしで作業することを嫌って参加を断る開発者が増えたので、AIで作業が速くなる度合いを低く見積もる偏りが入ったそうです。


# 1968年にもあった「ソフトウェア工場」

いまの「工場」という呼び方は製造業の無人工場に由来し、StrongDMやVercelの文書に昔のソフトウェア開発の話は出てきません。
ところが「ソフトウェア工場」という言葉そのものは58年前からあって、Addy Osmaniも[Software Factories, Light and Dark](https://addyosmani.com/blog/software-factories/)でこの言葉は1968年のBob Bemerまでさかのぼると書いています。

いまの工場が当時の工場から生まれたという話は見つからなかったのですが、同じ言葉で何が試されたのかが面白かったので紹介します。
1968年のNATOのソフトウェア工学会議の[報告書](https://web.archive.org/web/2016/http://homepages.cs.ncl.ac.uk/brian.randell/NATO/nato1968.PDF)では、General ElectricにいたBemerの作業文書を次のように引いています。

> By utilizing the machine-controlled production environment, or software factory. Program construction, checkout and usage are done entirely within this environment using the tools it contains. Ideally it should be impossible to produce programs exterior to this environment.

Bemerが工場と呼んだのは機械が管理する環境で、プログラムの作成と検査をすべてその中で行わせるものです。

翌1969年には日立がソフトウェアの部門を神奈川工場から独立させました。
日立の[沿革](https://www.hitachi.co.jp/Div/soft/history.html)ではこれを「世界で最初のソフトウェア専門事業所」と紹介していて、東芝や日本電気や富士通も1970年代に同じ方向へ進みます。

アメリカではSystem Development Corporationが1975年ごろに約200人規模のソフトウェア工場を作ったのですが、数年で使われなくなりました。
MITのMichael Cusumanoが1991年に書いた[草稿](https://www.gregorystrachta.com/resources/Touchstones/swp-3268-23661042.pdf)によると、顧客ごとのプロジェクトの責任者は仕様を工場に渡すより自分で作りたがり、プロジェクトの間で道具を使い回すこともできなかったそうです。

東芝の府中工場については成果が数字で残っています。
同じ草稿によると納めたコードのうち既存のコードを再利用した行の割合が1979年の13%から1985年の48%に上がり、東芝は生産性と品質が上がった主な理由を再利用だとしています。

同じころFred Brooksは1986年の[No Silver Bullet](https://www.cs.unc.edu/techreports/86-020.pdf)で次のように書きました。

> There is no single development, in either technology or management technique, which by itself promises even one order of magnitude improvement in productivity, in reliability, in simplicity.

ソフトウェアの難しさは仕様と設計と検査にあるので、コードを書き表しやすくするだけでは改善の桁は変わらないという指摘です。

2004年にはMicrosoftが[Software Factories](https://learn.microsoft.com/en-us/archive/blogs/askburton/moving-to-software-factories-2)という名前で、分野ごとの言語とひな型からアプリケーションを組み立てる道具をVisual Studioに載せようとしました。
記事ではほかの産業が職人仕事から部品を組み合わせる製造へ移って生産能力を増やしたと主張していて、東芝の再利用にも通じる考えです。

この記事ではsoftware factoryという言葉が創造性のない自動人形を連想させ、議論を呼ぶことも認めています。
道具を説明する[ページ](https://learn.microsoft.com/en-us/previous-versions/msp-n-p/ff648577(v=pandp.10))には2011年11月に古い内容になったという表示が付いています。

当時の工場が人の組織と手順を整えたのに対して、いまの工場はエージェントを動かす仕組みを作っていますが、比べてみると似ている点が3つありました。

Symphonyがタスクごとに用意する作業場や冒頭のポストの図にある「決まりに反する操作を実行前に止める」フックは、Bemerの「この環境の外ではプログラムを作れないようにする」と発想が似ています。

東芝は速く書くことよりも再利用を成果の理由に挙げていて、OpenAIは記事でlintと構造のテストを重視していました。
どちらも書く量を減らすことや書いたコードを確かめることに手間をかけています。

エージェントがコードを書くようになったあとも、Brooksが難しいとした仕様と検査は人が担っていて、StrongDMのシナリオはその検査に人が時間をかけた結果に見えます。


# 手元で組んでみた形

私も常時稼働させているデスクトップでVercel Boardを動かし、Tailscaleのネットワークを通じてノートPCと共有してみました。

私は指揮役のエージェントのスレッド1本だけに話しかけます。
指揮役はワーカーが1ターンで終えられる単位に仕事を分けてBoardにタスクを登録します。

デスクトップの管理サーバーは着手できる状態のタスクを取り出して、ラベルで指定されたPCでワーカーのエージェントを起こします。
1台のPCが同時に受け持つタスクは5件までで、ワーカーのターンが終わると管理サーバーは結果をタスクの状態に反映して指揮役に通知します。

設定とコードは[agent-board.nix](https://github.com/conao3/nixos-configuration/blob/master/home-manager/features/agent-board.nix)と[agent-board-server.py](https://github.com/conao3/nixos-configuration/blob/master/pkgs/agent-board-server.py)に置いてあります。
自分で書いたのは約500行のPythonの管理サーバーだけで、タスクを並べることと同じタスクを複数のワーカーに渡さないこと、結果を記録することはBoardに任せました。

タスクのスレッドに返信すると同じワーカーが続きを進め、作業の途中ならワーカーはそのターンの中で返信を受け取ります。
ブラウザで依頼を書いて返事をするだけで仕事を進められます。

数分おきに方針を変える企画の段階ではエージェントと直接話すほうが速く進みました。
方針が固まって切り出せる仕事は工場に向いています。

仕事を渡す前の仕様と受け取った成果を確かめる仕組みを、私が用意できるかどうかで結果が変わります。
次は検査を先に用意してからタスクを切り出す進め方を試すつもりです。

