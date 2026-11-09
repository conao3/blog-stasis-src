Title: LSPの仕様に厳格なクライアントとサーバーを作り、5つのエディタとrust-analyzerを調べた
Date: 2026-11-09 17:30
Draft: true


エディタと言語サーバーがLanguage Server Protocol、略してLSPの仕様どおりに通信しているかを確かめる、lsp-touchstoneを作りました。
仕様に厳格なクライアントと厳格なサーバー、通信を検査しながら中継するproxyの3つを1つのコマンドに入れています。
リポジトリは[conao3/rust-lsp-touchstone](https://github.com/conao3/rust-lsp-touchstone)で、ライセンスはApache-2.0です。
この記事は0.1.0で動かした結果をもとに書いています。

検査に使う型と、どのメソッドがどのcapabilityに対応するかは、LSP 3.18の仕様のメタモデル `metaModel.json` から読みます。
lsp-touchstoneを5つのエディタとrust-analyzerのあいだに置いたところ、どれにも仕様に合わない振る舞いが見つかりました。

<div class="github-card" data-repo="conao3/rust-lsp-touchstone"><a href="https://github.com/conao3/rust-lsp-touchstone"><span class="github-card-name"><svg class="github-card-mark" viewBox="0 0 16 16" aria-hidden="true"><path d="M6.766 11.328c-2.063-.25-3.516-1.734-3.516-3.656 0-.781.281-1.625.75-2.188-.203-.515-.172-1.609.063-2.062.625-.078 1.468.25 1.968.703.594-.187 1.219-.281 1.985-.281.765 0 1.39.094 1.953.265.484-.437 1.344-.765 1.969-.687.218.422.25 1.515.046 2.047.5.593.766 1.39.766 2.203 0 1.922-1.453 3.375-3.547 3.64.531.344.89 1.094.89 1.954v1.625c0 .468.391.734.86.547C13.781 14.359 16 11.53 16 8.03 16 3.61 12.406 0 7.984 0 3.563 0 0 3.61 0 8.031a7.88 7.88 0 0 0 5.172 7.422c.422.156.828-.125.828-.547v-1.25c-.219.094-.5.156-.75.156-1.031 0-1.64-.562-2.078-1.609-.172-.422-.36-.672-.719-.719-.187-.015-.25-.093-.25-.187 0-.188.313-.328.625-.328.453 0 .844.281 1.25.86.313.452.64.655 1.031.655s.641-.14 1-.5c.266-.265.47-.5.657-.656"/></svg><span>conao3/<strong>rust-lsp-touchstone</strong></span></span><span class="github-card-description">A strict LSP client, server and checking proxy for testing Language Server Protocol implementations</span><span class="github-card-footer"><span class="github-card-language"><span class="github-card-language-color" style="background-color: #dea584"></span>Rust</span><span class="github-card-star"><svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 .25a.75.75 0 0 1 .673.418l1.882 3.815 4.21.612a.75.75 0 0 1 .416 1.279l-3.046 2.97.719 4.192a.751.751 0 0 1-1.088.791L8 12.347l-3.766 1.98a.75.75 0 0 1-1.088-.79l.72-4.194L.818 6.374a.75.75 0 0 1 .416-1.28l4.21-.611L7.327.668A.75.75 0 0 1 8 .25Z"/></svg><span>Star</span><span class="github-card-star-count" hidden></span></span></span></a></div>


# 3つの使い方

lsp-touchstoneの使い方は3つあります。

![img](../../../../resources/public/img/ccc5ed-modes.svg)

-   clientはブラウザのweb UIから言語サーバーを起動し、リクエストや通知を手で送ります。双方向のメッセージと、メッセージごとの指摘がその場で表に出ます。
-   proxyはエディタと言語サーバーのあいだに入り、すべてのメッセージを検査しながら中継します。
-   serverはエディタの言語サーバーとして動き、進捗の表示、機能の動的な登録、設定の問い合わせ、補完の候補のさまざまな形、 `workspace/applyEdit` による編集をエディタに送ります。

serverは、エディタが編集を適用したあとに送る `didChange` から文書の中身を計算し、送った編集を当てた文書と一致するかを確かめます。
これで、画面を見なくてもエディタが編集を正しく適用したかが分かります。

3つとも同じ検査を使います。
検査の一覧は[docs/rules.md](https://github.com/conao3/rust-lsp-touchstone/blob/master/docs/rules.md)にあり、指摘は80種類です。
仕様が禁じることはerrorにし、仕様が許していても実装によって読み方が分かれることはwarningにしています。


# 何を検査するか

検査は次の範囲を扱います。

-   ヘッダーと `Content-Length` の形
-   JSON-RPC 2.0のメッセージの形と、リクエストと応答の対応
-   `initialize` から `shutdown` 、 `exit` までの順序
-   paramsとresultの型
-   双方が宣言したcapabilityと、実際に送ったメソッドの対応
-   開いている文書の中身と、位置を数える単位
-   キャンセルと進捗の表示

lsp-touchstoneは開いている文書の写しを持ち、 `didChange` を受けるたびに更新します。
写しがあるので、サーバーが返した位置が文字の途中を指していないか、存在する行を指しているかを確かめられます。
位置の単位の変換、文書の範囲の置き換え、進捗の表示の状態の移り方は、Lean 4で書いて正しさを証明し、[lean-rs](https://s.conao3.com/blog/2026/b43a06)でRustに変換して使っています。


# エディタの前に置く

proxyは、エディタに言語サーバーのコマンドとして渡します。
eglotなら次のように書きます。

```elisp
(add-to-list 'eglot-server-programs
             '(rust-ts-mode . ("lsp-touchstone" "proxy" "run-proxy"
                               "--web-ui-port" "4318"
                               "--server-command-args" "rust-analyzer")))
```

`--server-command-args` のあとには、本来の言語サーバーのコマンドを書きます。
`--web-ui-port` を付けると、通信をその場で表示するweb UIのURLを標準エラーに出します。
Neovim、Helix、VS Code、lsp-modeの設定は[docs/editors.md](https://github.com/conao3/rust-lsp-touchstone/blob/master/docs/editors.md)にあります。

clientのweb UIでrust-analyzerを起動し、初期化してからファイルを1つ開くと、次の指摘が出ます。

![img](../../../../resources/public/img/ccc5ed-finding.png)

rust-analyzerが `workspace/diagnostic/refresh` のリクエストを送っています。
仕様はこのリクエストに対応するクライアントのcapabilityを `workspace.diagnostics.refreshSupport` と定めています。
lsp-touchstoneのclientはこれを宣言していません。
lsp-touchstoneは、クライアントが宣言していないcapabilityのリクエストをサーバーが送ったとき、errorにします。


# 5つのエディタで試した

`make check-editors` を実行すると、eglot、lsp-mode、Neovim、Helix、VS Codeを人の操作なしに動かします。
それぞれのエディタを、lsp-touchstoneのserverと、proxyを通したrust-analyzerにつなぎます。
そのうえで、hover、補完、定義へのジャンプ、1行の入力、コードアクションによる編集、保存、終了の順に操作します。
Helixにはバッチで動かす仕組みがないので、tmuxにキー入力を送って操作します。
VS Codeの代わりには、VS Codeをソースからビルドして配布しているVSCodiumを使います。
VSCodiumを画面なしで起動し、拡張の中でMicrosoftのクライアントのライブラリ `vscode-languageclient` を動かします。

エディタとrust-analyzerはnixpkgsから入れました。
バージョンは次のとおりです。

-   Emacs 31.1、eglot 1.24.31、lsp-mode 20261020.1728
-   Neovim 0.12.6
-   Helix 25.07.1
-   VSCodium 1.143.17183、vscode-languageclient 11.0.0
-   rust-analyzer 2026-11-02

5つのエディタはすべてserverが送った編集を正しく適用しました。
編集のあとの `didChange` から計算した文書は、どのエディタでも編集を当てた文書と一致しました。

一方で、終わり方はエディタごとに違い、仕様に合わないものもありました。
LSPでは、クライアントが `shutdown` を送り、応答を受けてから `exit` を送って終わります。
`shutdown` を送ったあとのクライアントは、 `exit` のほかにリクエストも通知も送ってはいけません。

-   eglotは `exit` に `"params": {}` を付けて送ります。 `exit` はparamsを持たない通知なので、warningにしています。
-   lsp-modeは `exit` を送らずにサーバーのプロセスを終わらせます。回によっては `"params": null` を付けた `exit` を送ります。JSON-RPC 2.0のparamsはオブジェクトか配列で、 `null` は仕様に合いません。
-   Neovimはrust-analyzerとつないだとき、回によって `shutdown` のあとに `textDocument/semanticTokens` のリクエストを送ります。
-   VS Codeのクライアントは回によって `shutdown` のあとに `$/cancelRequest` を送ります。
-   Helixは回によって `exit` が届く前にサーバーのプロセスを終わらせます。
-   rust-analyzerはクライアントから受けたリクエストに答えないまま `shutdown` に応答し、終了します。

ほかにも次の振る舞いが見つかりました。

-   rust-analyzerはどのエディタとつないでも `workspace/diagnostic/refresh` を送ります。5つのエディタのどれも `refreshSupport` を宣言していません。
-   rust-analyzerは `window/workDoneProgress/create` の応答を受ける前に、そのtokenで進捗を送り始めます。
-   lsp-modeは、Emacsが最初の保存でファイルをバックアップの `main.rs~` に改名するとき、 `workspace/willRenameFiles` をサーバーに送ります。
-   lsp-modeは、サーバーからのリクエストを処理する途中でエラーが起きると、そのリクエストに応答しません。
-   位置を数える単位は、serverが `utf-32` 、 `utf-8` 、 `utf-16` の順に選ぶようにしています。eglot、lsp-mode、Neovim、Helixは `utf-32` を選び、VS Codeのクライアントは `utf-16` しか示しません。

「回によって」と書いたものは、何回か実行したうちの一部で出たものです。
それぞれの指摘の詳細はdocs/editors.mdにまとめています。

