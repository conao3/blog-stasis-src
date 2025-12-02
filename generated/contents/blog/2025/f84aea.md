Title: 日本語レポートから始めるorg-modeとEmacs
Date: 2025-12-02


この記事は[Emacs Advent Calendar 2025](https://qiita.com/advent-calendar/2025/emacs)の4日目の記事です。

[org-mode](https://orgmode.org/)は言わずと知れたEmacsのキラーフィーチャーですが、Emacsの学習曲線もさることながら、org-modeの学習曲線も同様に急峻であるため入門したくても踏み切れないという初心者の方がたくさん居るという声を聞きます。
しかし、タイトルにもありますが、日本語レポートから始めるorg-modeでゆるやかにorg-mode、しいてはEmacsに入門するのは私がとてもおすすめしている入門経路で、実は私もこの経路でorg-modeとEmacsに入門しています。

ぜひ怖がらずに入門してみましょう。


# 日本語レポートの題材

なんでも良いのですが、[東工大のC言語入門](https://tcs.c.titech.ac.jp/csbook/c_lang/chap1.html)を見つけたのでこれを使わせてもらうことにします。


# Emacsの起動とファイルの準備

Emacsをインストールして起動してください。
Nixをインストールしている方は以下のコマンドでEmacsがインストールされている環境に入ることができます。

    nix shell nixpkgs#emacs

その後、 `emacs` で起動できます。

    emacs

![img](/img/9ad97367-e768-4a84-8d87-b6c15c23801e.png)

`C-x C-f (find-file)` を実行してファイルを開きます。存在しないパスを指定すると空のファイルとして開くことができます。

![img](/img/c7e82a66-58d6-4e62-8145-705ad1fdeece.png)

ここでもしフォルダが存在していない場合は以下のようなメッセージが表示されます。

> Use M-x make-directory RET RET to create the directory and its parents

この通りに実行してフォルダを作ることもできますし、保存時にフォルダを作るか尋ねられるので、そこでyと答えてもOKです。

![img](/img/13c9c8c4-b7d3-4fdd-941f-1666826bcc11.png)

![img](/img/0fff68f2-5564-4403-8c5c-74e16fc015fe.png)


# org-modeの文法

[features](https://orgmode.org/features.html)のページがざっと全体像になっています。
レポートを書くという目的ではheadingの作り方といくつかの文字装飾の方法を学んでおくと良いでしょう。
markdownとの対応で記述します。


## headingの作り方

markdownでは `#` を使いますが、org-modeでは `*` を利用します。 `M-RET` で挿入することができ、heading上にポインタがある状態で `M-←` や `M-→` でレベルの変更ができます。

h1は `*` 、h2では `**` というように重ねて深いレベルを表現するところは同じです。

なおmarkdownと同じく、改行は無視され、空行を入れることで段落分けを表現します。
そのため、headingの間は自由な量の空行を入れることができます。

    * heading 1
    ほんぶん1
    ** heading 1.1
    ほんぶん1.1
    ** heading 1.2
    ほんぶん1.2
    
    * heading 2
    ** heading 2.1
    ** heading 2.2


## 文字装飾

(本当はそのまま表示したかったけどレンダリングを止められなかったのでコードブロックで。)

    | markdown     | org-mode     | rendered   |
    |--------------+--------------+------------|
    | =`code`=     | ==code==     | =code=     |
    | =*emphasis*= | =/emphasis/= | /emphasis/ |
    | =**strong**= | =*strong*=   | *strong*   |
    | =~~strike~~= | =+strike+=   | +strike+   |


## リスト

markdownと同じく `-` でリストを作ることができます。
`-` のリスト上にポインタがあるとき、 `M-RET` で追加の `-` を挿入することができます。
headingと同じく、リスト上にポインタを置いた上で `M-←` と `M-→` でリストの深さを変更することができます。

    - apple
    - banana
    - orange
      - orange2

-   apple
-   banana
-   orange
    -   orange2


## リンク

markdownでは `[link text](link)` と表現するところ、org-modeでは `[[link][link text]]` と表現します。
逆になっているので注意しましょう。

もしくは `link text` と書いて選択した後、 `C-c C-l (org-insert-link)` を実行することで、対話的にリンクを作ることができます。

[org-mode](https://orgmode.org/)へのリンクです。


## テーブル

テーブルはmarkdownと同じくasciiで表現します。

テーブルヘッダはあってもなくても良く、作る場合は `-` で区切ります。
ただし、この表記を全て自分で整えなくても良く、ある程度書いたら `C-c C-c` を押すことでorg-modeに整えてもらうことができます。

    | 名前    | 説明         |
    |---------+--------------|
    | table   | テーブル     |
    | caption | キャプション |

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="org-left" />

<col  class="org-left" />
</colgroup>
<thead>
<tr>
<th scope="col" class="org-left">名前</th>
<th scope="col" class="org-left">説明</th>
</tr>
</thead>
<tbody>
<tr>
<td class="org-left">table</td>
<td class="org-left">テーブル</td>
</tr>

<tr>
<td class="org-left">caption</td>
<td class="org-left">キャプション</td>
</tr>
</tbody>
</table>


## コードブロック

org-modeでのコードブロックは拡張性があるがゆえにに少し冗長に感じるかもしれません。
markdownでは `` ``` 言語 `` と `` ``` `` で囲むことによって表現しますが、org-modeでは `#+begin_src 言語` と `#+end_src` で囲うことによって表現します。

    #+begin_src python
    print("hello")
    #+end_src

    print("hello")

言語の指定がなく、単にコードブロックとして表現したい場合は `#+begin_example` と `#+end_example` で囲います。

    #+begin_example
    こーど
    ブロック
    #+end_example

    こーど
    ブロック


## 脚注

markdownでは `foo[^foo]` と `[^foo]: fooに対しての脚注` と書きますが、org-modeは `foo[fn::fooに対しての脚注]` と表記します。

こんな感じ<sup><a id="fnr.1" class="footref" href="#fn.1" role="doc-backlink">1</a></sup>になります。


## メタ設定

org文書に対してタイトルなどを指定することができます。
基本的にはファイルの先頭に書くことが多いです。

    #+title: 文書のタイトル
    #+author: conao3
    #+date: 2025-12-02

設定できる項目の一覧は[ドキュメント](https://orgmode.org/manual/Export-Settings.html )にあります。
設定できてもエクスポートに利用する形式でサポートされていないと単に無視されることもあります。


# レポートを書こう

ここまで学んだ文法を元にorg-modeでレポートを書いてみましょう。

    #+title: C言語入門01 - プログラミングの準備
    #+author: conao3
    #+date: 2025-12-02
    
    * 演習問題回答
    ** 演習1-1
    #+begin_example
    以下のプログラムをコンパイル・実行すると、どのような表示がされるか確認してください。
    
    #include <stdio.h>
    
    int main() {
        printf("2 * 8 = %d\n", 2 * 8);
        printf("36 = %d * %d\n", 3, 12);
        return 0;
    }
    #+end_example
    
    回答
    
    #+begin_example
    2 * 8 = 16
    36 = 3 * 12
    #+end_example
    
    ** 演習1-2
    #+begin_example
    以下のプログラムをコンパイルするとコンパイルエラーが発生します。
    コンパイルエラーが発生する理由を考えて、修正方法を考えてください。
    
    #include <stdio.h>
    
    int main() {
        printf("The quick brown fox
                jumps over
                the lazy dog.");
        return 0;
    }
    #+end_example
    
    文字列の終端なしに行の終わりになっているため、エラーが発生する。
    修正方法としては =\= を行末に置き、行継続していることを表現することでエラーを修正できる。
    
    #+begin_src C
        printf("The quick brown fox\
                jumps over\
                the lazy dog.");
    #+end_src
    
    ** 演習1-3
    
    #+begin_example
    以下のプログラムと同じ表示をするプログラムをprintf 関数を
    1 回しか使わずに記述してください。
    
    ヒント: \n は文字列の途中に書くこともできます。
    
    #include <stdio.h>
    
    int main() {
        printf("%d apples\n", 4);
        printf("%d apples\n", 9);
        printf("%d apples\n", 16);
        return 0;
    }
    #+end_example
    
    回答
    
    #+begin_src C
    #include <stdio.h>
    
    int main() {
        printf("%d apples\n%d apples\n%d apples\n", 4, 9, 16);
        return 0;
    }
    #+end_src
    
    #+begin_example
    4 apples
    9 apples
    16 apples
    #+end_example


# エクスポートしてみよう

残念ながらorg-modeの形式では教授は受け取ってくれません。
しかし、org-modeから種々のフォーマットに変換することでその問題を解決することができます。

テキストフォーマットはもちろん、markdownやhtml、man形式やLaTeX形式など様々なフォーマットが[org-mode本体によりサポート](https://orgmode.org/manual/Exporting.html)されています。
なお、筆者はこの機能でLaTeXにエクスポートし卒論を書きました。

さらにはコミュニティにより他にも様々なフォーマットへの変換がサポートされています。
エクスポートパッケージは `ox`- を付けることが慣習のため、[MELPAでこのクエリを入れて検索](https://melpa.org/#/?q=ox-)すると追加で実に40種ほどのエクスポート先を利用することができます。
その中には私が作成した [ox-zenn](https://zenn.dev/conao3/articles/ox-zenn-usage) もあります。

今回は簡単にテキストフォーマットのエクスポートを試してみましょう。
org-modeの文書を開いた状態で、 `C-c C-t (org-export-dispatch)` を実行します。

![img](/img/07531bdc-3d0d-4137-8f18-c297e5d5f2d7.png)

そうすると現在エクスポート可能な一覧が表示されるので、今回は `t A` と入力してasciiでのテキストフォーマットにエクスポートしてみましょう。その結果がこちらです。

                      ____________________________________
    
                       C言語入門01 - プログラミングの準備
    
                                     conao3
                      ____________________________________
    
    
                                   2025-12-02
    
    
    Table of Contents
    _________________
    
    1. 演習問題回答
    .. 1. 演習1-1
    .. 2. 演習1-2
    .. 3. 演習1-3
    
    
    1 演習問題回答
    ==============
    
    1.1 演習1-1
    ~~~~~~~~~~~
    
      ,----
      | 以下のプログラムをコンパイル・実行すると、どのような表示がされるか確認してください。
      | 
      | #include <stdio.h>
      | 
      | int main() {
      |     printf("2 * 8 = %d\n", 2 * 8);
      |     printf("36 = %d * %d\n", 3, 12);
      |     return 0;
      | }
      `----
    
      回答
    
      ,----
      | 2 * 8 = 16
      | 36 = 3 * 12
      `----
    
    
    1.2 演習1-2
    ~~~~~~~~~~~
    
      ,----
      | 以下のプログラムをコンパイルするとコンパイルエラーが発生します。
      | コンパイルエラーが発生する理由を考えて、修正方法を考えてください。
      | 
      | #include <stdio.h>
      | 
      | int main() {
      |     printf("The quick brown fox
      |             jumps over
      |             the lazy dog.");
      |     return 0;
      | }
      `----
    
      文字列の終端なしに行の終わりになっているため、エラーが発生する。修正方
      法としては `\' を行末に置き、行継続していることを表現することでエラー
      を修正できる。
    
      ,----
      | printf("The quick brown fox\
      |         jumps over\
      |         the lazy dog.");
      `----

このようにテキストファイルとしてフォーマットされ、目次やタイトルが挿入され、地の文は適切に折り返した状態で出力されます。
編集時はエディタ側で折り返すけれどもエクスポート時はこのように特定の桁で改行を入れておく方がテキストファイルとしては適しています。

UTF-8でエクスポートした結果がこちらです。

                      ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                       C言語入門01 - プログラミングの準備
    
                                     conao3
                      ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    
    
                                   2025-12-02
    
    
    Table of Contents
    ─────────────────
    
    1. 演習問題回答
    .. 1. 演習1-1
    .. 2. 演習1-2
    .. 3. 演習1-3
    
    
    1 演習問題回答
    ══════════════
    
    1.1 演習1-1
    ───────────
    
      ┌────
      │ 以下のプログラムをコンパイル・実行すると、どのような表示がされるか確認してください。
      │ 
      │ #include <stdio.h>
      │ 
      │ int main() {
      │     printf("2 * 8 = %d\n", 2 * 8);
      │     printf("36 = %d * %d\n", 3, 12);
      │     return 0;
      │ }
      └────
    
      回答
    
      ┌────
      │ 2 * 8 = 16
      │ 36 = 3 * 12
      └────

このようにUTF-8の文字が使われ、asciiのものと比べると少しリッチなフォーマットになりました。

最後にHTMLでのエクスポートを試してみましょう。 `C-c C-e` の後、 `h o` でHTMLファイルで書き出してブラウザで開くところまでを実行してくれます。

![img](/img/2e5a3654-004d-488b-adb9-01aba1bc9449.png)

このようなスタイルのWebページを見たことはないでしょうか。もしかしたらそのページはorg-modeから書き出されたページかもしれません。


# 発展

ここまでの内容でも普通に便利にorg-modeを利用できていると言って過言ではないと思います。
ここからorg-modeの機能を少しずつ学んで行くことができるでしょう。

この日本語レポートが書けるようになった読者に次の一歩としておすすめなorgの機能は `babel` です。
babelはいわゆる「文芸的プログラミング」をサポートする機能で、「[A Multi-Language Computing Environment for Literate Programming and Reproducible Research](https://www.jstatsoft.org/article/view/v046i03)」で紹介 (というかorg-modeで論文あるのすごくないか) されているところの「Code Evaluation」にあたる機能です。

![img](/img/ba9bb4da-34e1-4303-8181-be1f085fdffc.png)

この機能を使うことで文書内に任意のプログラミング言語のブロックを挿入し、エクスポート時に実行することで実際のエクスポートの内容を決定するということができるようになります。

例えば以下のように記述することでエクスポート時にC言語ソースのコンパイルから実行まで自動で行い、結果をその↓のコードブロックとして挿入するということが実現できます。

    回答
    
    #+begin_src C :includes stdio.h :results output :exports results
    printf("2 * 8 = %d\n", 2 * 8);
    printf("36 = %d * %d\n", 3, 12);
    #+end_src

エクスポートもしくはこのコードブロックで `C-c C-c` を押すと実行され、結果がコードブロックとして埋め込まれた状態がこちらです。

    #+begin_src C :includes stdio.h :results output :exports results
    printf("2 * 8 = %d\n", 2 * 8);
    printf("36 = %d * %d\n", 3, 12);
    #+end_src
    
    #+RESULTS:
    : 2 * 8 = 16
    : 36 = 3 * 12

`:exports results` の指定をしてあるため、エクスポートした文書には結果のみが出力されます。
レポートの回答としては結果のみが求められていたのでこれで良いですね。

という感じで `babel` を導入するのはジャンプが少ないかなと思います。
論文の図に戻ると、右側の矢印はExportで既に扱っているため、残すは左の矢印のみです。
これは `tangle` と呼ばれ、このコードブロックを「抽出」してソースファイル化するというものです。

抽出したソースファイルの場所も当然のことながら指定できるため、つまりorg文書というドキュメントから複数のソースファイルを書き出せるという機能です。

tangleの利用例についてはたけてぃが最近は活発に記事を書いてくれているので興味があればぜひ。
<https://www.google.com/search?q=tangle+site%253Ahttps%253A%252F%252Fwww.takeokunn.org>

org-modeの機能は今回扱った文書としての使い方が大きなものではありますが、同じくらい大きな機能として本格的なGTD (Get Things Done) での利用に耐えうる程のタスク管理機能があります。
正直筆者もまだ使いこなせていないので手に馴染んだらまた紹介しようと思います。


# まとめ

org-modeから始めるEmacs入門を書きました。
個人的には私のEmacs入門のパスがこれなので、ずっと書きたかった記事です。
Emacsでいきなりコードを書き始めるのではなく、まずは日本語から書いてみてはどうかという趣旨です。

この記事を見てorg-modeに、そしてEmacsに入門してみようかなという人が一人でも増えてくれたらいいなと思います。


# Footnotes

<sup><a id="fn.1" href="#fnr.1">1</a></sup> どんな感じ？
