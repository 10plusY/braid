(ns braid.ui.views.pages.users
  (:require [om.core :as om]
            [om.dom :as dom]
            [braid.ui.views.group-invite :refer [group-invite-view]]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.store :as store]))

(defn- group-users [data]
  (->> (data :users)
       vals
       (filter (fn [u] (some #{(data :open-group-id)} (u :group-ids))))))

(defn user-view
  [user subscribe]
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      [:div
        [:li
          [:a {:href (routes/user-page-path {:group-id @open-group-id
                                             :user-id (user :id)})}
            [:img.avatar {:style {:background-color (id->color (user :id))}
                          :src (user :avatar)}]
            [:p (user :nickname)]]]])))

(defn user-list-view
  [users subscribe]
  (fn []
    [:ul
      (for [user users]
        ^{:key (user :id)}
        [user-view users subscribe])]))

(defn users-page-view
  [{:keys [subscribe]}]
  (let [users (subscribe [:users])
        open-group-id (subscribe [:open-group-id])]
    (fn []
      [:div.page.users
        [:div.title "Users"]
        [:div.content
          (let [users-by-status (->> (group-users @users)
                                     (group-by :status))]
            [:div.description
              [:h2 "Online"]
              [user-list-view (users-by-status :online) subscribe]

              [:h2 "Offline"]
              [user-list-view (users-by-status :offline) subscribe]])

          [:h2 "Invite"]
          [group-invite-view @open-group-id]]])))
