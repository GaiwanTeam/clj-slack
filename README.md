# clj-slack

clj-slack is a Clojure library with several independent Slack libraries. These Slack libraries are designed with an intention to make them work together easily, which means that they both depend on common configuration, message format, and database schema.

Key features

- wrap Slack REST API with middleware handling cursor-based pagination and HTTP retry
- handle Slack markdown message format

## Requirement
This library requires Java 11.

## Usage

```clojure
(ns slack-api
  (:require [co.gaiwan.clj-slack.core :as clj-slack]))

;; export environment variable SLACK_TOKEN
(def conn (clj-slack/conn))

;; Using the pre-decorated API to get all the emoji
(clj-slack/get-emoji conn)
```

## Test
```
bin/kaocha
```

<!-- license-mpl -->
## License
&nbsp;
Copyright &copy; 2017-2021 Arne Brasseur and contributors
&nbsp;
Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
<!-- /license-epl -->
