(ns braid.common.state
  (:require
    [reagent.ratom :include-macros true :refer-macros [reaction]]
    [clojure.set :refer [union intersection subset?]]))

(defn set-active-group-id!
  [state [_ group-id]]
  (assoc state :open-group-id group-id))

(defn get-active-group
  [state _]
  (let [group-id (reaction (:open-group-id @state))]
    (reaction (get-in @state [:groups @group-id]))))

(defn get-groups
  [state _]
  (reaction (vals (:groups @state))))

(defn get-group-admins
  ([state [_ group-id]]
   (reaction (get-in @state [:groups group-id :admins])))
  ([state _ [group-id]]
   (reaction (get-in @state [:groups group-id :admins]))))

(defn get-user
  "Get user by id. Can be sub'd directly or dynamically"
  ([state [_ user-id]]
   (reaction (get-in @state [:users user-id])))
  ([state _ [user-id]]
   (reaction (get-in @state [:users user-id]))))

(defn get-users
  [state _]
  (reaction (@state :users)))

(defn user-is-group-admin?
  "Is the given user an admin in the given group?"
  ([state [_ user-id group-id]]
   (reaction (contains? (get-in @state [:groups group-id :admins])
                        user-id)))
  ([state _ [user-id group-id]]
   (reaction (contains? (get-in @state [:groups group-id :admins])
                        user-id))))

(defn current-user-is-group-admin?
  "Is the current user an admin in the given group?"
  ([state [_ group-id]]
   (current-user-is-group-admin? state nil [group-id]))
  ([state _ [group-id]]
   (reaction (->> (get-in @state [:session :user-id])
                  (contains? (set (get-in @state [:groups group-id :admins])))))))

(defn- thread-unseen?
  [thread]
  (> (->> (thread :messages)
          (map :created-at)
          (apply max))
     (thread :last-open-at)))

(defn get-open-thread-ids
  [state _]
  (reaction (get-in @state [:user :open-thread-ids])))

(defn get-group-unread-count
  [state [_ group-id]]
  (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
        threads (reaction (@state :threads))
        tags (reaction (@state :tags))
        users (reaction (@state :users))
        group-ids->user-ids (reaction (->> @users
                                           vals
                                           (mapcat (fn [u]
                                                     (map
                                                       (fn [gid]
                                                         {:id (u :id) :group-id gid})
                                                       (u :group-ids))))
                                           (group-by :group-id)
                                           (map (fn [[k vs]]
                                                  [k (map (fn [v] (v :id)) vs)]))
                                           (into {})))
        group-user-ids (set (@group-ids->user-ids group-id))
        thread-in-group? (fn [thread]
                           (if (seq (thread :tag-ids))
                             (= group-id (:group-id (@tags (first (thread :tag-ids)))))
                             (let [user-ids-from-messages (->> (thread :messages)
                                                               (map :user-id)
                                                               set)
                                   user-ids-from-refs (set (thread :user-ids))
                                   user-ids (union user-ids-from-messages
                                                   user-ids-from-refs)]
                               (< 0 (count (intersection group-user-ids user-ids))))))
        unseen-threads (reaction
                         (->>
                           (select-keys @threads @open-thread-ids)
                           vals
                           (filter thread-unseen?)
                           (filter thread-in-group?)))]
    (reaction (count @unseen-threads))))

(defn get-page
  [state _]
  (reaction (@state :page)))

(defn get-threads
  [state _]
  (reaction (@state :threads)))

(defn get-page-id
  [state _]
  (reaction (get-in @state [:page :id])))

(defn get-open-threads
  [state [_]]
  (let [group-id (reaction (get-in @state [:open-group-id]))
        open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
        tag-id->group-id (reaction
                           (into {} (map (juxt :id :group-id))
                                 (vals (@state :tags))))
        threads (reaction (@state :threads))
        open-threads (reaction (vals (select-keys @threads @open-thread-ids)))
        group-users (reaction
                      (into #{}
                            (comp
                              (filter (fn [u] (contains? (set (u :group-ids)) @group-id)))
                              (map :id))
                            (vals (@state :users))))]
    (reaction
      (doall
        (filter (fn [thread]
                  (and
                    (or (empty? (thread :tag-ids))
                        (contains?
                          (into #{} (map @tag-id->group-id) (thread :tag-ids))
                          @group-id))
                    (subset?
                      (set (thread :mentioned-ids))
                      @group-users)))
                @open-threads)))))

(defn get-users-in-group
  [state [_ group-id]]
  (reaction
    (->> (@state :users)
         vals
         (filter (fn [u] (contains? (set (u :group-ids)) group-id)))
         doall)))

(defn get-open-group-id
  [state _]
  (reaction (get-in @state [:open-group-id])))

(defn get-users-in-open-group
  [state [_ status]]
  (reaction (->> @(get-users-in-group state [nil (@state :open-group-id)])
                 (filter (fn [u] (= status (u :status))))
                 doall)))

(defn get-user-id
  [state _]
  (reaction (get-in @state [:session :user-id])))

(defn get-all-tags
  [state _]
  (reaction (vals (get-in @state [:tags]))))

(defn get-user-subscribed-to-tag
  ([state [_ tag-id]]
   (reaction (contains? (set (get-in @state [:user :subscribed-tag-ids])) tag-id)))
  ([state _ [tag-id]]
   (reaction (contains? (set (get-in @state [:user :subscribed-tag-ids])) tag-id))))

(defn get-group-subscribed-tags
  [state _]
  (reaction
    (into ()
          (comp
            (filter (fn [tag] @(get-user-subscribed-to-tag state [nil (tag :id)])))
            (filter (fn [tag] (= (get-in @state [:open-group-id]) (tag :group-id)))))
          (vals (get-in @state [:tags])))))

(defn get-user-avatar-url
  [state [_ user-id]]
  (reaction (get-in @state [:users user-id :avatar])))

(defn get-user-status
  ([state [_ user-id]]
   (reaction (get-in @state [:users user-id :status])))
  ([state _ [user-id]]
   (reaction (get-in @state [:users user-id :status]))))

(defn get-search-query
  [state _]
  (reaction (get-in @state [:page :search-query])))

(defn get-tags-for-thread
  [state [_ thread-id]]
  (let [tag-ids (reaction (get-in @state [:threads thread-id :tag-ids]))
        tags (reaction (doall
                         (map (fn [thread-id]
                                (get-in @state [:tags thread-id])) @tag-ids)))]
    tags))

(defn get-mentions-for-thread
  [state [_ thread-id]]
  (let [mention-ids (reaction (get-in @state [:threads thread-id :mentioned-ids]))
        mentions (reaction (doall
                             (map (fn [user-id]
                                    (get-in @state [:users user-id])) @mention-ids)))]
    mentions))

(defn get-messages-for-thread
  [state [_ thread-id]]
  (reaction (get-in @state [:threads thread-id :messages])))

(defn get-thread-open?
  [state [_ thread-id]]
  (reaction (contains? (set (get-in @state [:user :open-thread-ids])) thread-id)))

(defn get-thread-new-message
  ([state [_ thread-id]] (get-thread-new-message state nil [thread-id]))
  ([state _ [thread-id]]
   (reaction (if-let [th (get-in @state [:threads thread-id])]
               (get th :new-message "")
               (get-in @state [:new-thread-msg thread-id] "")))))

(defn get-errors
  [state _]
  (reaction (get-in @state [:errors])))

(defn get-login-state
  [state _]
  (reaction (get-in @state [:login-state])))

(defn get-tag
  ([state [_ tag-id]]
   (reaction (get-in @state [:tags tag-id])))
  ([state _ [tag-id]]
   (reaction (get-in @state [:tags tag-id]))))

(defn get-group-for-tag
  [state [_ tag-id]]
  (reaction (get-in @state [:tags tag-id :group-id])))

(defn get-threads-for-group
  ([state [_ group-id]] (get-threads-for-group state nil [group-id]))
  ([state _ [group-id]]
   (let [group-for-tag (fn [tag-id]
                         (get-in @state [:tags tag-id :group-id]))
         group-users (reaction
                       (into #{}
                             (comp
                               (filter (fn [u] (contains? (set (u :group-ids)) group-id)))
                               (map :id))
                             (vals (@state :users))))]
     (reaction (->> (@state :threads)
                    vals
                    (filter (fn [thread]
                              (and (or (empty? (thread :tag-ids))
                                       (contains?
                                         (into #{} (map group-for-tag) (thread :tag-ids))
                                         group-id))
                                (subset?
                                  (set (thread :mentioned-ids))
                                  @group-users))))
                    doall)))))

(defn get-nickname
  ([state [_ user-id]] (get-nickname state nil [user-id]))
  ([state _ [user-id]]
   (reaction (get-in @state [:users user-id :nickname]))))

(defn get-invitations
  [state _]
  (reaction (get-in @state [:invitations])))

(defn get-pagination-remaining
  [state _]
  (reaction (@state :pagination-remaining)))

(defn get-user-subscribed-tag-ids
  [state _]
  (reaction (set (get-in @state [:user :subscribed-tag-ids]))))

(defn get-connected?
  [state _]
  (reaction (not-any? (fn [[k _]] (= :disconnected k)) (@state :errors))))

(defn get-new-thread-id
  [state _]
  (reaction (get @state :new-thread-id)))

(defn get-preference
  [state [_ pref]]
  (reaction (get-in @state [:preferences pref])))

(defn get-calls
  [state _]
  (reaction (@state :calls)))

(defn get-call-status?
  ([state [_ call-id]] get-call-status? state nil call-id)
  ([state _ call-id]
   (reaction (get-in @state [:calls call-id :status]))))
