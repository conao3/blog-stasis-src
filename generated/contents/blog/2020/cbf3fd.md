Title: MELPAのパッケージページにHomepageリンクが追加
Date: 2020-05-27


MELPAの公式がツイートしてた。

<blockquote class="twitter-tweet"><p lang="en" dir="ltr">We changed the MELPA website&#39;s package pages so that &quot;Homepage&quot; links go to the URL named in the package&#39;s Homepage or URL header, instead of directly to the source code. Hope that helps you explore more conveniently!</p>&mdash; MELPA (@melpa&#95;emacs) <a href="https://twitter.com/melpa&#95;emacs/status/1265125454282801153?ref&#95;src=twsrc%5Etfw">May 26, 2020</a></blockquote> <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>

どうやらこの[コミット](https://github.com/melpa/melpa/commit/02163bf1d920d076f927803796b0f86e3f13ef9b)らしい。この[issue](https://github.com/melpa/melpa/issues/6914)が引用されています。
さらにこの[ML](https://lists.gnu.org/archive/html/emacs-devel/2020-05/msg02470.html)の投稿がリンクされている。投稿者は[Emms](https://www.gnu.org/software/emms/)のメンテナであるYuniさん。

ポイントは以下。

    What the MEPLA people are doing that I don't like:
    
        * Never communicate with the developers of the Emms in any way.
    
        * Omit many files that come with Emms.
    
        * Associate Emms with several Emms extensions that live only on
          MELPA and that we, the Emms developers, have never heard
          about. This would give anyone accessing Emms via MELPA that those
          extensions are somehow a part of Emms, when they are not.
    
          Maybe those extensions are good, in which case I would love for
          the developers to contact us, the Emms developers. But Maybe those
          extensions are bad, don't work, are out of date, or connect with
          non-free services.
    
        * Not even linking to the Emms home page
          (https://www.gnu.org/software/emms/).

    Ideas for improvement:
    
        * Encourage people to speak to the developers of a project before
          packaging it.
    
        * Find a way of packaging a project as-is. For instance, Emms could
          be distributed as is, and the M/ELPA software could simply point
          at where Emms keeps its .el files for Emacs to find. This is
          instead of how I see ELPA working now, which is to force the
          software through a kind of a sieve (I think ELPA calls it a
          recipe) where only a select few files come out the other end.
    
          Emms doesn't need a recipe; it already comes organized and
          packaged for working with Emacs.
    
          It makes me think of taking bread, crumbling it up, the mushing
          those crumbs back together to re-form a new loaf of bread.

この投稿を受けて、MELPAにHomepageリンクの追加が提案されました。

![img](/img/cbf3fd-0afef58e-178d-4706-9fea-89841b6d8958.png)

このようにHomepageというボタンが追加されました。

ただ、、MLは個人的にあまり使いたくない。。

そもそも[量が膨大](https://lists.gnu.org/archive/html/emacs-devel/2020-05/threads.html)すぎるし、この量をさばくUIもない。
本家の開発に参加したいのは山々なのですが、本質とは関係ないところで敷居を高くしている気がします。

