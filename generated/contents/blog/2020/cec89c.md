Title: リストに関数を適用して最初にnon-nilを返した値を得る方法
Date: 2020-05-26


このような関数をさくっと使いたい。
3つのポイントがあり、「リストに関数を適用して」「最初にnon-nilが返ってきた」「その値を得る」ということ。

2つめのポイントまでなら `cl-find-if` がそのまま使える。
`cl-find-if` は「リストに関数を適用して」「最初にnon-nilが返ってきた」「元の値を得る」ことができる。

    (cl-find-if (lambda (elm) (when (<= 3 elm) (* elm elm))) '(1 2 3 4))
    ;;=> 3

私が欲しいのはこういう関数です。

    (defun first-value (fn lst)
      "Apply FN for LST and return first non-nil value."
      (funcall fn (cl-find-if fn lst)))
    ;;=> first-value
    
    (first-value (lambda (elm) (when (<= 3 elm) (* elm elm))) '(1 2 3 4))
    ;;=> 9

Emacs-jpで聞いたところ、koshさんに `seq-some` を教えてもらいました。

    (seq-some (lambda (elm) (when (<= 3 elm) (* elm elm))) '(1 2 3 4))
    ;;=> 9

便利。

