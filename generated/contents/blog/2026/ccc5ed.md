Title: LSPの仕様違反は、画面に出ないところに残っていた
Date: 2026-11-09 17:30
Draft: true


エディタと言語サーバーをつなぐLanguage Server Protocol、略してLSPは、仕様どおりに実装されているでしょうか。
これを確かめるために、仕様に厳格なクライアントとサーバー、通信を検査しながら中継するproxyを作り、eglot、lsp-mode、Neovim、Helix、VS Codeの5つのエディタとrust-analyzerの通信を調べました。
仕様どおりに動いていたのは、使う人の目に見えるところだけでした。
編集の適用のように画面に結果が出る機能は、5つのエディタがすべて正しく動きました。
一方で、終わり方や機能の宣言のように画面に何も出ないところは、どの実装も少しずつ仕様から外れていました。

作った道具はlsp-touchstoneです。
リポジトリは[conao3/rust-lsp-touchstone](https://github.com/conao3/rust-lsp-touchstone)で、ライセンスはApache-2.0です。
この記事は0.1.0で動かした結果をもとに書いています。

<div class="github-card" data-repo="conao3/rust-lsp-touchstone"><a href="https://github.com/conao3/rust-lsp-touchstone"><span class="github-card-name"><svg class="github-card-mark" viewBox="0 0 16 16" aria-hidden="true"><path d="M6.766 11.328c-2.063-.25-3.516-1.734-3.516-3.656 0-.781.281-1.625.75-2.188-.203-.515-.172-1.609.063-2.062.625-.078 1.468.25 1.968.703.594-.187 1.219-.281 1.985-.281.765 0 1.39.094 1.953.265.484-.437 1.344-.765 1.969-.687.218.422.25 1.515.046 2.047.5.593.766 1.39.766 2.203 0 1.922-1.453 3.375-3.547 3.64.531.344.89 1.094.89 1.954v1.625c0 .468.391.734.86.547C13.781 14.359 16 11.53 16 8.03 16 3.61 12.406 0 7.984 0 3.563 0 0 3.61 0 8.031a7.88 7.88 0 0 0 5.172 7.422c.422.156.828-.125.828-.547v-1.25c-.219.094-.5.156-.75.156-1.031 0-1.64-.562-2.078-1.609-.172-.422-.36-.672-.719-.719-.187-.015-.25-.093-.25-.187 0-.188.313-.328.625-.328.453 0 .844.281 1.25.86.313.452.64.655 1.031.655s.641-.14 1-.5c.266-.265.47-.5.657-.656"/></svg><span>conao3/<strong>rust-lsp-touchstone</strong></span></span><span class="github-card-description">A strict LSP client, server and checking proxy for testing Language Server Protocol implementations</span><span class="github-card-footer"><span class="github-card-language"><span class="github-card-language-color" style="background-color: #dea584"></span>Rust</span><span class="github-card-star"><svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 .25a.75.75 0 0 1 .673.418l1.882 3.815 4.21.612a.75.75 0 0 1 .416 1.279l-3.046 2.97.719 4.192a.751.751 0 0 1-1.088.791L8 12.347l-3.766 1.98a.75.75 0 0 1-1.088-.79l.72-4.194L.818 6.374a.75.75 0 0 1 .416-1.28l4.21-.611L7.327.668A.75.75 0 0 1 8 .25Z"/></svg><span>Star</span><span class="github-card-star-count" hidden></span></span></span></a></div>


# 調べ方

lsp-touchstoneには、3つの動かし方があります。

![img](../../../../resources/public/img/ccc5ed-modes.svg)

-   clientはブラウザのweb UIから言語サーバーを起動し、リクエストや通知を手で送ります。
-   proxyはエディタと言語サーバーのあいだに入り、すべてのメッセージを検査しながら中継します。
-   serverはエディタの言語サーバーとして動き、仕様にあるサーバーからの機能を順に使います。

検査に使う型と、どのメソッドがどのcapabilityに対応するかは、LSP 3.18の仕様のメタモデル `metaModel.json` から読みます。
指摘は[docs/rules.md](https://github.com/conao3/rust-lsp-touchstone/blob/master/docs/rules.md)の80種類です。
仕様が禁じることはerrorにし、仕様が許していても実装によって読み方が分かれることはwarningにしています。

`make check-editors` を実行すると、5つのエディタを人の操作なしに動かします。
それぞれのエディタを、lsp-touchstoneのserverと、proxyを通したrust-analyzerにつなぎます。
そのうえで、hover、補完、定義へのジャンプ、1行の入力、コードアクションによる編集、保存、終了の順に操作します。
エディタとrust-analyzerはnixpkgsから入れました。
バージョンは次のとおりです。

-   Emacs 31.1、eglot 1.24.31、lsp-mode 20261020.1728
-   Neovim 0.12.6
-   Helix 25.07.1
-   VS Codeのクライアントのライブラリ `vscode-languageclient` 11.0.0。VS Codeをソースからビルドして配布しているVSCodium 1.143.17183で動かしました
-   rust-analyzer 2026-11-02


# 編集の適用

serverはコードアクションから `workspace/applyEdit` でエディタに編集を送ります。
エディタが編集を適用すると、エディタは次の `didChange` で文書の変更をサーバーに伝えます。
serverは `didChange` から文書の中身を計算し、送った編集を当てた文書と一致するかを確かめます。

5つのエディタはすべて、編集を正しく適用しました。
編集がおかしければ画面ですぐ分かるので、この部分はよく確かめられているのだと思います。


# 終わり方

LSPでは、クライアントが `shutdown` を送り、応答を受けてから `exit` を送って終わります。
`shutdown` を送ったあとのクライアントは、 `exit` のほかにリクエストも通知も送ってはいけません。
この手順を、6つの実装がそれぞれ違う形で扱っていました。

-   eglotは `exit` に `"params": {}` を付けて送ります。 `exit` はparamsを持たない通知なので、warningにしています。
-   lsp-modeは `exit` を送らずにサーバーのプロセスを終わらせます。回によっては `"params": null` を付けた `exit` を送ります。JSON-RPC 2.0のparamsはオブジェクトか配列で、 `null` は仕様に合いません。
-   Neovimはrust-analyzerとつないだとき、回によって `shutdown` のあとに `textDocument/semanticTokens` のリクエストを送ります。
-   VS Codeのクライアントは回によって `shutdown` のあとに `$/cancelRequest` を送ります。
-   Helixは回によって `exit` が届く前にサーバーのプロセスを終わらせます。
-   rust-analyzerはクライアントから受けたリクエストに答えないまま `shutdown` に応答し、終了します。

「回によって」と書いたものは、何回か実行したうちの一部で出たものです。
どれも、エディタを閉じる途中で起きます。
使う人はエディタを閉じようとしているので、サーバーが後片付けを飛ばしても、応答のないリクエストが残っても、気づく場面がありません。


# 宣言していない機能

LSPでは、クライアントとサーバーが `initialize` で、自分が扱える機能をcapabilityとして宣言します。
相手が宣言していない機能のリクエストは、送っても相手が扱えるとは限りません。

clientのweb UIでrust-analyzerを起動し、初期化してからファイルを1つ開くと、次の指摘が出ます。

![img](../../../../resources/public/img/ccc5ed-finding.png)

rust-analyzerが `workspace/diagnostic/refresh` のリクエストを送っています。
仕様はこのリクエストに対応するクライアントのcapabilityを `workspace.diagnostics.refreshSupport` と定めていて、lsp-touchstoneのclientはこれを宣言していません。

rust-analyzerは5つのエディタのどれとつないでもこのリクエストを送りました。
5つのエディタはどれも `refreshSupport` を宣言していません。
それでも、eglot、lsp-mode、Neovim、VS Codeは `"result": null` で成功を返しました。
Helixは「メソッドが見つからない」というエラーを返し、rust-analyzerはそのまま動き続けました。
どちらの場合も、使う人の画面には何も出ません。

rust-analyzerは `window/workDoneProgress/create` の応答を受ける前に、そのtokenで進捗を送り始めることもあります。
これも、エディタが進捗をそのまま表示するので、画面の上では問題が見えません。


# 違反が残る理由

LSPの実装を確かめる相手は、多くの場合、実際のエディタやサーバーです。
実際の相手は仕様に合わないメッセージを受けても止まらずに受け流すので、違反をした側には何も返ってきません。
画面で確かめられる機能は使う人が不具合に気づきますが、画面に出ない部分は誰も気づかないまま残ります。

違反を見つけるには、受け流さない相手が要ります。
lsp-touchstoneは仕様に合わないメッセージを受けるたびに、どの規則に反したかをその場で示します。


# 使い方

proxyはエディタに言語サーバーのコマンドとして渡します。
eglotなら次のように書きます。

```elisp
(add-to-list 'eglot-server-programs
             '(rust-ts-mode . ("lsp-touchstone" "proxy" "run-proxy"
                               "--web-ui-port" "4318"
                               "--server-command-args" "rust-analyzer")))
```

`--server-command-args` のあとには、本来の言語サーバーのコマンドを書きます。
`--web-ui-port` を付けると、通信をその場で表示するweb UIのURLを標準エラーに出します。
lsp-mode、Neovim、Helix、VS Codeの設定と、エディタごとの結果の詳細は[docs/editors.md](https://github.com/conao3/rust-lsp-touchstone/blob/master/docs/editors.md)にあります。

