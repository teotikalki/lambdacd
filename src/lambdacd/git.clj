(ns lambdacd.git
  (:require [lambdacd.shell :as sh]
            [lambdacd.execution :as execution]
            [lambdacd.util :as util]
            [clojure.tools.logging :as log]))

(defn- current-revision [repo-uri branch]
  (.trim (:out (sh/bash "/" (str "git ls-remote --heads " repo-uri " " branch " | cut -f 1")))))


(defn- revision-changed-from [last-seen-revision repo-uri branch]
  (fn []
    (let [revision-now (current-revision repo-uri branch)]
      (log/debug "waiting for new revision. current revision" revision-now "last seen" last-seen-revision)
      (not= last-seen-revision revision-now))))

(defn wait-for-git [repo-uri branch]
  (let [last-seen-revision (current-revision repo-uri branch)]
    (execution/wait-for (revision-changed-from last-seen-revision repo-uri branch))))

(defn- checkout [repo-uri revision]
  (let [cwd (util/create-temp-dir)]
    (sh/bash cwd (str "git clone " repo-uri " .") (str "git checkout " revision))
    cwd))


(defn with-git [repo-uri steps]
  (fn [args step-id]
    (let [repo-location (checkout repo-uri (:revision args))] ;; TODO: wouldn't it be better to pass in the revision?
      (execution/execute-steps steps (assoc args :cwd repo-location) (execution/new-base-id-for step-id)))))

