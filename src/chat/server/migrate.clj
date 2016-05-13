(ns chat.server.migrate
  (:require [chat.server.db :as db]
            [datomic.api :as d]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.edn :as edn]))

(defn migrate-2016-05-13
  "Threads have associated groups"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :thread/group
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])
    (let [threads (->> (d/q '[:find [?t ...]
                              :where
                              [?t :thread/id]]
                            (d/db db/*conn*))
                       (d/pull-many
                         (d/db db/*conn*)
                         [:thread/id
                          {:thread/mentioned [:user/id]}
                          {:thread/tag [:tag/group]}
                          {:message/_thread [:message/created-at
                                             {:message/user [:user/id]}]}]))
          tx
          (vec
            (for [th threads]
              (let [author (->> (th :message/_thread)
                                (sort-by :message/created-at)
                                first
                                :message/user)
                    author-grp (some-> author :user/id
                                       db/get-groups-for-user
                                       first :id)
                    fallback-group (:group/id (d/pull (d/db db/*conn*) [:group/id] [:group/name "Braid"]))]
                (cond
                  (seq (th :thread/tag))
                  (let [grp (get-in th [:thread/tag 0 :tag/group :db/id])]
                    (when (nil? grp)
                      (println "Nil by tag " (th :thread/id)))
                    [:db/add [:thread/id (th :thread/id)]
                     :thread/group grp])

                  (seq (th :thread/mentioned))
                  (let [grps (apply
                               set/intersection
                               (map (comp :id
                                          db/get-groups-for-user
                                          :user/id)
                                    (cons author (th :thread/mentioned))))
                        grp (or (first grps) author-grp fallback-group)]
                    (when (nil? grp)
                      (println "Nil by mentions " (th :thread/id)))
                    [:db/add [:thread/id (th :thread/id)]
                     :thread/group [:group/id grp]])

                  :else
                  (let [grp (or author-grp fallback-group)]
                    (when (nil? grp)
                      (println "nil by author" (th :thread/id)))
                    [:db/add [:thread/id (th :thread/id)]
                     :thread/group [:group/id grp]])))))]
      (d/transact db/*conn* tx))))

(defn migrate-2016-05-07
  "Change how user preferences are stored"
  []
  (db/with-conn
    ; rename old prefs
    (d/transact db/*conn* [{:db/id :user/preferences
                            :db/ident :user/preferences-old}])
    ; create new entity type
    (d/transact db/*conn*
      [{:db/ident :user/preferences
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}

       {:db/ident :user.preference/key
        :db/valueType :db.type/keyword
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :user.preference/value
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])

    ; migrate to new style
    (let [prefs (d/q '[:find (pull ?u [:user/id :user/preferences-old])
                       :where [?u :user/id]]
                     (d/db db/*conn*))]
      (doseq [[p] prefs]
        (let [u-id (:user/id p)
              u-prefs (edn/read-string (:user/preferences-old p))]
          (doseq [[k v] u-prefs]
            (when k (db/user-set-preference! u-id k v))))))))

(defn migrate-2016-05-03
  "Add tag descriptions"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :tag/description
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))

(defn migrate-2016-04-29
  "Add group admins"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :group/admins
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))

(defn migrate-2016-03-28
  "Add group settings"
  []
  (db/with-conn
    (d/transact db/*conn*
                [{:db/ident :group/settings
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/id #db/id [:db.part/db]
                  :db.install/_attribute :db.part/db}])))

(defn migrate-2016-03-21
  "Add user preferences"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :user/preferences
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))

(defn migrate-2016-03-04
  "Add extension type as attribute"
  []
  (db/with-conn
    (d/transact db/*conn*
                [{:db/ident :extension/type
                 :db/valueType :db.type/keyword
                 :db/cardinality :db.cardinality/one
                 :db/id #db/id [:db.part/db]
                 :db.install/_attribute :db.part/db}])))

(defn migrate-2016-03-02
  "Add extension user"
  []
  (db/with-conn
    (d/transact db/*conn*
                [{:db/ident :extension/user
                 :db/valueType :db.type/ref
                 :db/cardinality :db.cardinality/one
                 :db/id #db/id [:db.part/db]
                 :db.install/_attribute :db.part/db}])))

(defn migrate-2016-02-26
  "Add extension schema"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :extension/id
        :db/valueType :db.type/uuid
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/identity
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/group
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/token
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/refresh-token
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/config
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/watched-threads
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       ])))

(defn migrate-2016-01-14
  "All users must have a nickname"
  []
  (db/with-conn
    (let [give-nicks (->> (d/q '[:find (pull ?u [:user/id :user/email :user/nickname])
                                 :where
                                 [?u :user/id]]
                               (d/db db/*conn*))
                          (map first)
                          (filter (comp nil? :user/nickname))
                          (mapv (fn [u] [:db/add [:user/id (:user/id u)]
                                         :user/nickname (-> (:user/email u) (string/split #"@") first)])))]
      (d/transact db/*conn* give-nicks))))

(defn migrate-2016-01-01
  "Change email uniqueness to /value, add thread mentions"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/id :user/email
        :db/unique :db.unique/value
        :db.alter/_attribute :db.part/db}
       {:db/ident :thread/mentioned
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))

(defn migrate-2015-12-19
  "Add user nicknames"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :user/nickname
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/value
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))

(defn migrate-2015-12-12
  "Make content fulltext"
  []
  ; rename content
  (db/with-conn (d/transact db/*conn* [{:db/id :message/content :db/ident :message/content-old}]))
  (db/with-conn (d/transact db/*conn* [{:db/ident :message/content
                                        :db/valueType :db.type/string
                                        :db/fulltext true
                                        :db/cardinality :db.cardinality/one
                                        :db/id #db/id [:db.part/db]
                                        :db.install/_attribute :db.part/db}]))
  (let [messages (db/with-conn (->> (d/q '[:find (pull ?e [:message/id
                                                           :message/content-old
                                                           :message/created-at
                                                           {:message/user [:user/id]}
                                                           {:message/thread [:thread/id]}])
                                           :where [?e :message/id]]
                                         (d/db db/*conn*))
                                    (map first)))]
    (db/with-conn
      (let [msg-tx (->> messages
                        (map (fn [msg]
                               [:db/add [:message/id (msg :message/id)]
                                :message/content (msg :message/content-old)])))]
        (d/transact db/*conn* (doall msg-tx))))))

(defn migrate-2015-07-29
  "Schema changes for groups"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :tag/group
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}

       ; groups
       {:db/ident :group/id
        :db/valueType :db.type/uuid
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/identity
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :group/name
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/identity
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :group/user
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}]))
  (println "You'll now need to create a group and add existing users & tags to that group"))

(defn create-group-for-users-and-tags
  "Helper function for migrate-2015-07-29 - give a group name to create that
  group and add all existing users and tags to that group"
  [group-name]
  (db/with-conn
    (let [group (db/create-group! {:id (db/uuid) :name group-name})
          all-users (->> (d/q '[:find ?u :where [?u :user/id]] (d/db db/*conn*)) (map first))
          all-tags (->> (d/q '[:find ?t :where [?t :tag/id]] (d/db db/*conn*)) (map first))]
      (d/transact db/*conn* (mapv (fn [u] [:db/add [:group/id (group :id)] :group/user u]) all-users))
      (d/transact db/*conn* (mapv (fn [t] [:db/add t :tag/group [:group/id (group :id)]]) all-tags)))))

(defn migrate-2015-08-26
  "schema change for invites"
  []
  (db/with-conn
    (d/transact db/*conn*
      [
       ; invitations
       {:db/ident :invite/id
        :db/valueType :db.type/uuid
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/identity
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :invite/group
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :invite/from
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :invite/to
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :invite/created-at
        :db/valueType :db.type/instant
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       ])))
