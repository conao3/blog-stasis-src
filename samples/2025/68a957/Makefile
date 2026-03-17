.PHONY: all
all:

.PHONY: repl
repl:
	clj -M:dev:nrepl:repl

.PHONY: update
update:
	clojure -Sdeps '{:deps {org.slf4j/slf4j-simple {:mvn/version "RELEASE"} com.github.liquidz/antq {:mvn/version "RELEASE"}}}' -M -m antq.core --upgrade --force

.PHONY: test
test:
	clojure -M:dev -m kaocha.runner
