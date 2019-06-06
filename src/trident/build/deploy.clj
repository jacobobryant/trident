(ns trident.build.deploy
  (:require [trident.cli :refer [make-cli]]
            [trident.build.pom :refer [sync-pom]]
            [trident.build.jar :refer [jar]]
            [trident.build.lib :refer [cli-options jar-file]]
            [deps-deploy.deps-deploy :as deps-deploy]))

(defn- lib-task [command {:keys [skip-jar] :as opts}]
  (assert (contains? #{"install" "deploy"} command))
  (when (not skip-jar)
    (println "generating pom")
    (sync-pom opts)
    (println "packaging")
    (jar opts))
  (deps-deploy/-main command (jar-file opts)))
(def install (partial lib-task "install"))
(def deploy (partial lib-task "deploy"))

(let [subcommand
      (fn [f cmd desc]
        (make-cli
          {:fn f
           :prog (str "clj -m trident.build.deploy " cmd)
           :desc [desc (str "Packages the jar first by default. The jar path is "
                            "`target/<artifact-id>-<version>.jar`.")]
           :config ["lib.edn"]
           :cli-options [:group-id :artifact-id :version :github-repo :skip-jar]}
          cli-options))

      {install-cli :cli install-help :help}
      (subcommand install "install" "Installs a library to the local maven repo.")

      {deploy-cli :cli deploy-help :help}
      (subcommand install "deploy"
                  ["Deploys a library to Clojars."
                   (str "The environment variables `CLOJARS_USERNAME` and "
                        "`CLOJARS_PASSWORD` must be set.")])

      {:keys [cli main-fn help]}
      (make-cli {:prog "clj -m trident.build.deploy"
                 :subcommands {"install" install-cli "deploy" deploy-cli}})]
  (def cli cli)
  (def ^{:doc help} -main main-fn)
  (alter-meta! #'install assoc :doc install-help)
  (alter-meta! #'deploy assoc :doc deploy-help))
