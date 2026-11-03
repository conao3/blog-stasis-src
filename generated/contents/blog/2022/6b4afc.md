Title: dioxus-tui入門 - 0.2.2
Date: 2022-12-16



# はじめに

この記事はRustアドベントカレンダー16日目の記事です。
昨日はyukinaritさんの「[PGOでRustで書かれた広告サーバを早くする](https://zenn.dev/yukinarit/articles/71bb4a01a8e58e)」でした。

RustでTUIアプリケーションを作りたいなーっと思っていろいろ探していると dioxus-tui というライブラリを見つけたので、使ってみた記事を書きます。

今回の記事で作ったレポジトリは [conao3/rust-dioxus-tui-sample](https://github.com/conao3/rust-dioxus-tui-sample) に置いてあります。


# dioxusとは

TUIアプリケーション専用ではなく、仮想DOMを使ってWebフロントエンド、デスクトップアプリ、モバイルアプリを書き出すクレート。

日本語記事ではここらへんを参照

-   [dioxus web やってみる - Zenn/mizchi](https://zenn.dev/mizchi/scraps/449f0591e2cee9)
-   [Rust UIライブラリ「Dioxus」をReact+Typescriptと比較してみた - Qiita/uwattotaitai](https://qiita.com/uwattotaitai/items/4d262e46c2165e01f477)

この記事ではdioxusを使ってTUIアプリを作ってみます。


# Getting Started

適当なRustプロジェクトを作って遊んでみます。

    mkdir rust-dioxus-tui-sample
    cd rust-dioxus-tui-sample
    cargo init --name dioxus-tui-sample

cargoで取ってきます。

    cargo add dioxus dioxus-tui

`src/main.rs` を以下で置き換えます。

    use dioxus::prelude::*;
    
    fn main() {
        dioxus_tui::launch(app);
    }
    
    fn app(cx: Scope) -> Element {
        cx.render(rsx! {
            div {
                width: "100%",
                height: "10px",
                background_color: "red",
                justify_content: "center",
                align_items: "center",
                "Hello world!"
            }
        })
    }

cargoで起動します。

    cargo run

起動できたら成功です! :tada:
![img](/img/6b4afc-b58d9c16-6660-40dc-b655-03cba36e2cd9.png)


# rsx!マクロ


## rsx

JSXのようなインターフェースを提供するマクロ。次のようなサンプルが動きます。

    use dioxus::prelude::*;
    
    fn main() {
        dioxus_tui::launch(app);
    }
    
    fn app(cx: Scope) -> Element {
        let val = 42;
        let name = if val == 42 { "Jack" } else { "Bob" };
    
        cx.render(rsx! (
            div {
                "hello world"
                "hello {val}"
                "hello {name}"
            }
        ))
    }

なおJSXのように `{}` の中に任意の式を書くことはできないようです。一旦rsxマクロの外で計算しておくのがおすすめとのこと。


## rsx - bool.then()

一方でこういうのはできるようです。

    let show_title = true;
    rsx!(
        div {
            // Renders nothing by returning None when show_title is false
            show_title.then(|| rsx!{
                "This is the title"
            })
        }
    )


## rsx - Option.map()

`Option` に対する `map` 風のAPIも使えます。

    let user_name = Some("bob");
    rsx!(
        div {
            // Renders nothing if user_name is None
            user_name.map(|name| rsx!("Hello {name}"))
        }
    )


## rsx - iter

動的に `rsx!` を作ることもできます。

    let names = ["jim", "bob", "jane", "doe", "a"];
    
    cx.render(rsx! (
        ul {
            names.iter().map(|name| rsx!{
                li { "{name}" }
            })
        }
    ))

が、tuiのレンダラは上手く表示できないようです。(改行せずに上書き表示されてる？)
![img](/img/6b4afc-5df13c92-b44b-4268-b735-9be1fc8acff9.png)


## rsx - Components

Componentの実装

    #![allow(non_snake_case)]
    
    use dioxus::prelude::*;
    
    #[derive(Props)]
    struct TitleCardProps<'a> {
        title: &'a str,
    }
    
    fn TitleCard<'a>(cx: Scope<'a, TitleCardProps<'a>>) -> Element {
        cx.render(rsx!{
            h1 { "{cx.props.title}" }
        })
    }
    
    fn main() {
        dioxus_tui::launch(app);
    }
    
    fn app(cx: Scope) -> Element {
        cx.render(rsx! (
            TitleCard {
                title: "hello world"
            }
        ))
    }

![img](/img/6b4afc-fb43d7c3-2ef6-47a5-8219-ab07b80abc9e.png)


# hooks

React風のhookが利用できます。


## useState

`use_state` はReactと全く同じです。

    let count = use_state(&cx, || 0);
    
    cx.render(rsx! (
        div {
            "count: {count}"
        }
    ))

本来ならボタンを追加して値の更新をするのですが。。dioxus-tuiにはまだボタンが実装されていなく確かめられませんでした。


## useFuture

dioxusの拡張。async関数を取って、非同期で実行してくれます。

    use dioxus::prelude::*;
    
    fn main() {
        dioxus_tui::launch(app);
    }
    
    fn app(cx: Scope) -> Element {
        let count = use_state(&cx, || 0);
    
        use_future(&cx, (), move |_| {
            let count = count.to_owned();
            let update = cx.schedule_update();
            async move {
                loop {
                    count.with_mut(|f| *f += 1);
                    tokio::time::sleep(std::time::Duration::from_millis(1000)).await;
                    update();
                }
            }
        });
    
        cx.render(rsx! {
            div { width: "100%",
                div {
                    width: "50%",
                    height: "5px",
                    background_color: "blue",
                    justify_content: "center",
                    align_items: "center",
                    "Hello {count}!"
                }
                div {
                    width: "50%",
                    height: "10px",
                    background_color: "red",
                    justify_content: "center",
                    align_items: "center",
                    "Hello {count}!"
                }
            }
        })
    }

![img](/img/6b4afc-acd498f4-5230-45d9-a4b5-2cbb086bcd58.png)

良く見るとこのサンプル、背景赤の場所に表示されるべき文字が左に出てますね。。


# Events

`on*` を使ってイベントを処理できる。。らしい。が、dioxus-tui(0.2.2)ではpanicしてしまった。

    let log_event = move |event: Event| {
        events.write().push(event);
    };
    
    
    cx.render(rsx! {
        div {
            width: "100%",
            height: "100%",
            flex_direction: "column",
            div {
                width: "80%",
                height: "50%",
                border_width: "1px",
                justify_content: "center",
                align_items: "center",
                background_color: "hsl(248, 53%, 58%)",
    
                onmousemove: move |event| log_event(Event::MouseMove(event.data)),
                onclick: move |event| log_event(Event::MouseClick(event.data)),
                ondblclick: move |event| log_event(Event::MouseDoubleClick(event.data)),
                onmousedown: move |event| log_event(Event::MouseDown(event.data)),
                onmouseup: move |event| log_event(Event::MouseUp(event.data)),
    
                onwheel: move |event| log_event(Event::Wheel(event.data)),
    
                onkeydown: move |event| log_event(Event::KeyDown(event.data)),
                onkeyup: move |event| log_event(Event::KeyUp(event.data)),
                onkeypress: move |event| log_event(Event::KeyPress(event.data)),
    
                onfocusin: move |event| log_event(Event::FocusIn(event.data)),
                onfocusout: move |event| log_event(Event::FocusOut(event.data)),
    
                "Hover, click, type or scroll to see the info down below"
            },
            div {
                width: "80%",
                height: "50%",
                flex_direction: "column",
                events_rendered,
            },
        },
    })

![img](/img/6b4afc-30ae77b3-bf8c-4dc8-abaf-410cbbf70074.png)

<https://github.com/DioxusLabs/dioxus/blob/fc2aaa7df5/packages/tui/examples/tui_all_events.rs>

レポジトリに置いてあるHEADのサンプルも同じくpanicしてしまうのでどこかのコミットで壊れてそのままになってる。。かなにか動かし方を間違えてるようです。

動かせた人は教えてくれると嬉しいです。


# まとめ

dioxusという意欲的なプロジェクトを見つけたのでちょっと動かしてみました。

TUIのレンダラはまだまだ発展途上のようですが、React文化をTUIに持ってくるのは普通に便利だと思うので期待してます。

(ユーザーとしては素直にnodeの[ink](https://github.com/vadimdemedes/ink)を使った方がいいかもしれない。ただ俺はRustでTUIを作りたいんだ。。!)

v1がリリースされたらまたチャレンジしてみようかなと思います。

