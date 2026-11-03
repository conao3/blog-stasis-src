Title: org-modeドキュメントからZenn Flavored Markdownを生成するox-zennの使い方
Date: 2020-09-25



# tl;dr

-   [ox-zenn](https://github.com/conao3/ox-zenn.el)というorgからZenn flavored markdownを生成するパッケージを書きました。
-   Zennマークダウンは独自記法があるので、orgファイルとの対応について少し注意することがある。


# org-modeとは


## 概要

「[org-mode](https://orgmode.org/)とはEmacsのメジャーモードのひとつです」という説明は正しいのですが、org-modeの力を説明するには全く言葉が足りません。

[org-modeの日本語プロジェクトページ](https://orgmode.org/ja/)には

> Org-mode はノートの保存、TODOリストの管理、プロジェクト計画、文書編集のためのモードです。高速で効率的なプレーンテキストのシステムを使ってファイルを編集します。

と説明されています。

この説明でとても重要なのはorgドキュメントが「プレーンテキスト」であるという点です。

「プレーンテキスト」はロックインを避けるための究極の選択肢です。

「org-mode」にロックインしてるじゃないか。という手痛い指摘が飛んできそうですが、それには当たりません。なぜならorg-modeが編集の対象にしているのは「プレーンテキスト」であり、もしorg-modeが明日なくなったとしても普通のエディタで編集や読み書きができ、様々なUnixコマンドによる処理が容易できるからです。


## 万能のドキュメントソース

org-modeはとても大きく、私も全容を把握できていません。
しかしつまみ食いをするだけでもとても大きなメリットがある機能があります。
それがorgのエクスポート機能とコード実行機能<sup><a id="fnr.1" class="footref" href="#fn.1" role="doc-backlink">1</a></sup>です。

![img](/img/6dae6b-92e07c45-cc71-48c6-b009-1e7e1b7a524d.png)

このようにorgドキュメントを中心として、HTMLやLaTeX、Wordドキュメント生成や、実行可能なソースコードを生成する。ドキュメントの中で任意の言語のコード実行を伴なうこともできます。

LSP(Language Server Protocol Support)にも似た思想を感じます。
LSPも各言語に対するプログラミング支援環境を各エディタにそれぞれ実装するのは無駄が多いからという理由で現代では主流といっても良い、勢いとシェアがあります。

私がorgドキュメントとorg-modeを使う理由も同じです。
もうHTMLを、LaTeXを、Wordを、PowerPointをそれぞれのファイルフォーマットで編集する時代は終わっているのです。
私が編集するドキュメント形式はorgドキュメントだけであり、orgドキュメントに情報を集約することで情報の再利用性と利便性が向上するのです。


# ox-zenn

orgはorgドキュメントをパースし、それぞれの「意味」でどのような「出力」にするのか制御することでエクスポート先のファイルを生成します。

つまり我々がorgドキュメントから新しいフォーマットで出力したい場合はorgが解釈した「意味」からどのような出力にするのかというエクスポートバックエンドのみを作成すれば良いことになります。
それがこちら「[ox-zenn](https://github.com/conao3/ox-zenn.el)」です。

ox-zennの作り方については別の記事にするとして、この記事では使い方を説明します。


## org-modeとox-zennのインストール

[leaf](https://github.com/conao3/leaf.el)を使ってインストールします。
また、orgはEmacsに標準添付されているのですが、upstreamがとても活発に改善されているので、この手順に従い、最新版を入れることをおすすめします。

現状、ox-zennがまだ[celpa](http://celpa.conao3.com/)にしか入っていないので、celpaのURLを `package-archives` に追加する必要があることに注意する必要があります。

    (eval-and-compile
      (customize-set-variable
       'package-archives '(("gnu" . "https://elpa.gnu.org/packages/")
                           ("melpa" . "https://melpa.org/packages/")
                           ("celpa" . "https://celpa.conao3.com/packages/")
                           ("org" . "https://orgmode.org/elpa/")))
      (package-initialize)
      (unless (package-installed-p 'leaf)
        (package-refresh-contents)
        (package-install 'leaf))
    
      (leaf leaf-keywords
        :ensure t
        :config
        (leaf-keywords-init)))
    
    (leaf org
      :ensure org-plus-contrib
      :custom ((org-startup-indented . t)
               (org-return-follows-link . t)
               (org-src-window-setup . 'other-window)
               (org-highlight-latex-and-related . '(latex script entities))))
    
    (leaf ox-zenn
       :ensure t
       :after org
       :require t)


## 基本のorg記法

orgのプロジェクトページに[org-syntax](https://orgmode.org/worg/dev/org-syntax.html)のページがあるので参照して頂ければと。


## ox-zenn拡張記法

Zenn公式による[Markdownの解説記事](https://zenn.dev/zenn/articles/markdown-guide)があります。
Zennは基本的に今時のマークダウン、つまり[GFM(GitHub Flavored Markdown)](https://github.github.com/gfm/)を受け取るように見えます。
さらにZenn独自の記法として[独自拡張のブロック要素](https://zenn.dev/zenn/articles/markdown-guide#zenn%E7%8B%AC%E8%87%AA%E3%81%AE%E8%A8%98%E6%B3%95)と[コンテンツ埋め込み用のショートコード](https://zenn.dev/zenn/articles/markdown-guide#%E5%A4%96%E9%83%A8%E3%82%B3%E3%83%B3%E3%83%86%E3%83%B3%E3%83%84%E3%81%AE%E5%9F%8B%E3%82%81%E8%BE%BC%E3%81%BF)をサポートしています。

残念ながらorgには対応している記法が存在しないのでox-zennが特別に解釈する文法を追加しました。
もちろん、他のバックエンドは解釈しないのでドキュメントの可搬性が落ちることに留意する必要があります。


### frontmatter

マークダウンの上にYAMLのfrontmatterが書けるのは、どのマークダウンが始めたのか分かりませんが、Zennのマークダウンもfrontmatterでタイトルなどのメタ要素を指定することができます。

Zennはfrontmatterで次の要素を指定することができます。

-   `title`
-   `topics`
-   `emoji`
-   `type`
-   `published`

orgドキュメントでは次のように指定すると、

    #+title: ZennのMarkdown記法
    
    #+gfm_tags: markdown zenn
    #+gfm_custom_front_matter: :emoji 👩‍💻 :type tech
    #+gfm_custom_front_matter: :published true

次のようにマークダウンに出力されます。

    ---
    author: Naoya Yamashita
    title: "ZennのMarkdown記法"
    last_modified: 2020-09-24
    emoji: 👩‍💻
    type: tech
    topics: [markdown, zenn]
    published: true
    ---

`gfm_tags` は空白区切りです。
`gfm_custom_front_matter` はplist風に空白区切りで指定します。
それぞれ、一行が長くなる場合、複数行に分けて指定しても正しく解釈されます。

`author` と `last_modified` は自動で追加されるので、それらは `options` で無効化できます。

    #+title: ZennのMarkdown記法
    #+options: broken-links:mark toc:nil author:nil last-modified:nil
    
    #+gfm_tags: markdown zenn
    #+gfm_custom_front_matter: :emoji 👩‍💻 :type tech
    #+gfm_custom_front_matter: :published true

ZennではサイドバーにToCが表示されているので、 `toc:nil` を指定しておくのも便利です。
`:topics` の指定において、空白が含まれる値を設定する都合上、若干トリッキーになっているので注意する必要があります。


### ブロック要素

ブロック要素はブロッククオートにhtml属性を付けることで対応しました。
このように定義することで他のバックエンドで出力した際にも多少は意味を保てると思います。

-   **message:** ブロッククオートに `#+attr_html:` で `x-type` に `message` を指定します。
    
        #+attr_html: :x-type message
        #+begin_quote
        メッセージをここに
        #+end_quote
    
    これは次のようにマークダウンに変換され、
    
        :::message
        メッセージをここに
        :::
    
    次のように表示されます。
    
    > メッセージをここに

-   **alert:** ブロッククオートに `#+attr_html:` で `x-type` に `alert` を指定します。
    
        #+attr_html: :x-type alert
        #+begin_quote
        メッセージをここに
        #+end_quote
    
    これは次のようにマークダウンに変換され、
    
        :::alert
        メッセージをここに
        :::
    
    次のように表示されます。
    
    > メッセージをここに

-   **details:** ブロッククオートに `#+attr_html:` で `x-type` に `details` を指定します。summaryを指定したい場合は `x-summary` にサマリを指定します。
    
        #+attr_html: :x-type details :x-summary タイトル
        #+begin_quote
        表示したい内容
        #+end_quote
    
    これは次のようにマークダウンに変換され、
    
        :::details タイトル
        表示したい内容
        :::
    
    次のように表示されます。
    
    > 表示したい内容
    
    なお、 `x-summary` 要素を省略した場合は `details` をサマリに指定したと解釈されます。


### 埋め込みショートコード

ショートコードは悩んだ結果、それぞれのサービス名をschemeとして指定するリンクを特別に解釈することにしました。

つまりorgで次のように記述した場合、

    [[tweet://https://twitter.com/conao_3/status/1308747297790898176]]
    [[youtube://jNa3axo40qM]]
    [[codepen://https://codepen.io/alphardex/pen/poyOMgr]]
    [[slideshare://866aBtwVYswXvu]]
    [[speakerdeck://3491c9ab20ef40938119aecadb06f1c6]]
    [[jsfiddle://https://jsfiddle.net/chrisvfritz/50wL7mdz]]

次のようにマークダウンで出力され、

    @[tweet](https://twitter.com/conao_3/status/1308747297790898176)
    @[youtube](jNa3axo40qM)
    @[codepen](https://codepen.io/alphardex/pen/poyOMgr)
    @[slideshare](866aBtwVYswXvu)
    @[speakerdeck](3491c9ab20ef40938119aecadb06f1c6)
    @[jsfiddle](https://jsfiddle.net/chrisvfritz/50wL7mdz)

次のように表示されます。

<https://twitter.com/conao_3/status/1308747297790898176>

<https://www.youtube.com/watch?v=jNa3axo40qM>

<https://codepen.io/alphardex/pen/poyOMgr>

<https://www.slideshare.net/slideshow/embed_code/key/866aBtwVYswXvu>

<https://speakerdeck.com/player/3491c9ab20ef40938119aecadb06f1c6>

<https://jsfiddle.net/chrisvfritz/50wL7mdz>

なお、この拡張は他のバックエンドで動きません。
またデフォルトの状態ではこのリンクは未定義要素へのリンクとして解釈されるため、他のドキュメントへのエクスポートが途中で止まってしまいます。。

これは未定義要素へのリンクを無視するように指定することで回避できます。
orgドキュメントの上の方に `#+options: broken-links:mark` と記述します。


### 画像の拡張記法

orgにおいて、画像を表示するにはdescriptionなしのリンクで表現します。

さらにZennは独自の記法により[画像の横幅を指定](https://zenn.dev/zenn/articles/markdown-guide#%E7%94%BB%E5%83%8F)できるようになっています。
この機能については `#+attr_html` で `alt` 属性と `width` 属性を指定すると適切に出力するようにしました。

つまりこのorgフォーマットは

    [[https://raw.githubusercontent.com/conao3/files/master/blob/headers/png/ox-zenn.el.png]]
    
    #+attr_html: :alt image
    [[https://raw.githubusercontent.com/conao3/files/master/blob/headers/png/ox-zenn.el.png]]
    
    #+attr_html: :alt image :width 250px
    [[https://raw.githubusercontent.com/conao3/files/master/blob/headers/png/ox-zenn.el.png]]

このように変換され、

    ![](https://raw.githubusercontent.com/conao3/files/master/blob/headers/png/ox-zenn.el.png)
    ![image](https://raw.githubusercontent.com/conao3/files/master/blob/headers/png/ox-zenn.el.png)
    ![image](https://raw.githubusercontent.com/conao3/files/master/blob/headers/png/ox-zenn.el.png =250x)

このように表示されます。

![img](/img/6dae6b-ox-zenn.el.png)

![img](/img/6dae6b-ox-zenn.el.png)

![img](/img/6dae6b-ox-zenn.el.png)

注意点は画像っぽいリンク、正確に言えば `org-html-inline-image-rules` にマッチするリンクでないとインライン表示されず、単なるリンクになってしまいます。

    org-html-inline-image-rules
    ;;=> (("file" . "\\.\\(jpeg\\|jpg\\|png\\|gif\\|svg\\)\\'")
    ;;    ("attachment" . "\\.\\(jpeg\\|jpg\\|png\\|gif\\|svg\\)\\'")
    ;;    ("http" . "\\.\\(jpeg\\|jpg\\|png\\|gif\\|svg\\)\\'")
    ;;    ("https" . "\\.\\(jpeg\\|jpg\\|png\\|gif\\|svg\\)\\'"))


## org-publishサポート

この章はオプショナルです。

ox-zennはorg-publishをサポートしているので、若干の設定が必要になりますが、こちらの方が便利だという向きもあるかもしれません。

    (leaf ox-zenn
      :ensure t
      :after org
      :require t ox-publish
      :defun zenn/f-parent org-publish
      :defvar org-publish-project-alist
      :preface
      (defvar zenn/org-dir "~/dev/repos/zenn-src/org")
    
      (defun zenn/org-publish (arg)
        "Publish zenn blog files."
        (interactive "P")
        (let ((force (or (equal '(4) arg) (equal '(64) arg)))
              (async (or (equal '(16) arg) (equal '(64) arg))))
          (org-publish "zenn" arg force async)))
    
      :config
      (setf
       (alist-get "zenn" org-publish-project-alist nil nil #'string=)
       (list
        :base-directory (expand-file-name "" zenn/org-dir)
        :base-extension "org"
        :publishing-directory (expand-file-name "../" zenn/org-dir)
        :recursive t
        :publishing-function 'org-zenn-publish-to-markdown)))

こうすれば次のようなディレクトリ構成にした上で、orgディレクトリ以下のorgドキュメントを一括でarticlesディレクトリとbooksディレクトリにマークダウンに変換できます。(1記事/1orgドキュメント)

    ~/dev/repos/zenn-src/
    ├── articles/
    │   ├── (ox-zenn-usage.md)
    │   ├── (ox-publish-usage.md)
    │   ├── (ox-odt-usage.md)
    │   └── (ox-html-usage.md)
    ├── books/
    │   └── ox-zenn-book/
    │       ├── (1.md)
    │       ├── (2.md)
    │       ├── (3.md)
    │       ├── (config.yml)
    │       └── (cover.png)
    ├── org/
    │   ├── articles/
    │   │   ├── ox-zenn-usage.org
    │   │   ├── ox-publish-usage.org
    │   │   ├── ox-odt-usage.org
    │   │   └── ox-html-usage.org
    │   └── articles
    │       └── ox-zenn-book/
    │           ├── 1.org
    │           ├── 2.org
    │           ├── 3.org
    │           ├── config.yml
    │           └── cover.png
    └── README.org

()で囲われたファイルが生成対象のファイル群です。
orgディレクトリでのディレクトリ構造のままファイルがpublishされていることが分かります。


## subtreeスタイル

この章もオプショナルです。

私はこの方法でZennコンテンツを管理することにしました。

ディレクトリ構造はこのようになります。

    ~/dev/repos/zenn-src/
    ├── articles/
    │   ├── (ox-zenn-usage.md)
    │   ├── (ox-publish-usage.md)
    │   ├── (ox-odt-usage.md)
    │   └── (ox-html-usage.md)
    ├── books/
    │   └── ox-zenn-book/
    │       ├── (1.md)
    │       ├── (2.md)
    │       ├── (3.md)
    │       ├── config.yml
    │       └── cover.png
    ├── main.org
    └── README.org

()で囲われたファイルが生成対象のファイル群です。

main.orgに全てのコンテンツを集中させる方法(多記事/1orgドキュメント)です。
ox-hugoのような積極的なサポートではないですが、org標準の機能を使ってもこのように記事を管理できます。

main.orgは次のような形です。

    #+title: Zenn blog source
    #+author: conao3
    #+date: <2020-09-24 Thu>
    #+options: ^:{} toc:nil
    
    * articles
    ** org-modeドキュメントからZenn Flavored Markdownを生成するox-zennの使い方
    :PROPERTIES:
    :EXPORT_FILE_NAME: articles/ox-zenn-usage
    :EXPORT_GFM_TAGS: markdown zenn
    :EXPORT_GFM_CUSTOM_FRONT_MATTER: :emoji 👩‍💻 :type tech
    :EXPORT_GFM_CUSTOM_FRONT_MATTER+: :published true
    :END:
    
    *** org-modeとは
    **** 概要
    「[[https://orgmode.org/][org-mode]]とはEmacsのメジャーモードのひとつです」という説明は正しいのですが、org-modeの力を説明するには全く言葉が足りません。
    ...

単一orgドキュメントで管理するようになるので、各記事は第2レベルをトップレベルとするようになり、記事ごとのオプションはプロパティドロワで設定するようになります。

プロパティには `EXPORT_` が前置されることに注意してください。

エクスポートは記事タイトル(つまり第2レベルの見出し)にポイントを乗せた上で、 `C-c C-e C-s z z` という呪文()を入力すれば出力できます。


# まとめ

ox-zennを使うことでorgドキュメントからZenn拡張の含まれたマークダウンを出力できるようになりました。
ぜひみなさんもこのバックエンドを使ってorgからZennの記事を書いてもらえればと思います。

気に入ってもらえたら、ぜひzennか[patreon](https://www.patreon.com/conao3)でサポートを頂ければと思います!


# Footnotes

<sup><a id="fn.1" href="#fnr.1">1</a></sup> Eric Schulte, Dan Davison, Thomas Dye, et.al., "A Multi-Language Computing Environment for Literate Programming and Reproducible Research" <https://www.jstatsoft.org/article/view/v046i03>
