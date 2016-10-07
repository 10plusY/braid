(ns braid.client.register.views
  (:require
    [garden.core :refer [css]]
    [garden.stylesheet :refer [at-import]]))

(def braid-color "#2bb8ba")

(def small-spacing "0.5rem")
(def border-radius "3px")

(defn input-field-mixin []
  {:font-size "1.25rem"
   :font-family "Open Sans"
   :padding "0.4em"
   :border "1px solid #ddd"
   :line-height 1.5
   :border-radius border-radius})

(defn form-styles []
  [:form.register
   {:display "flex"
    :flex-direction "column"
    :height "100vh"
    :padding [["3rem" 0 "1.5rem"]]
    :box-sizing "border-box"
    :justify-content "space-around"}

   [:h1
    {:margin 0
     :color braid-color
     :font-weight "normal"
     :font-size "1.75em"
     :margin-bottom "0.75rem"}

    [:&:before
     {:content "\"\""
      :display "inline-block"
      :margin-right "0.5em"
      :margin-bottom "-0.15em"
      :margin-left "-1.75em"
      :width "1.25em"
      :height "1.25em"
      :background-image "url(/images/braid-logo-color.svg)"
      :background-size "contain"}]]

   [:.option
    {:margin [[small-spacing 0]]}

    [:h2
     {:font-size "1em"
      :margin [[0 0 small-spacing 0]]}]

    [:.explanation
     {:color "#999"
      :font-size "0.75em"
      :margin [[small-spacing 0 0 0]]}]

    [:p
     {:margin "0"}]

    [:label
     {:display "block"}

     ["input[type=text]"
      (input-field-mixin)

      [:&:focus
       {:border-color braid-color
        :outline "none"}]]

     ["::-webkit-input-placeholder"
      {:color "#eee"}]]

    [:&.group-url

     [:.domain
      {:white-space "nowrap"}

      ["input[type=text]"
       {:text-align "right"
        :border-radius [[border-radius 0 0 border-radius]]
        :width "7.5em"
        :vertical-align "top" }]

      [:span
       (input-field-mixin)
       {:border-left "none"
        :display "inline-block"
        :vertical-align "top"
        :background "#f6f6f6"
        :color "#999"
        :border-radius [[0 border-radius border-radius 0]]}

       [:&::after
        {:content "\"\""
         :width "0.15em"
         :display "inline-block"}]]]]

    [:&.group-type

     [:label
      {:margin [[small-spacing 0]]
       :border "1px solid #eee"
       :padding [["0.75rem" "1rem" "1.0rem"]]
       :border-radius border-radius}

      [:span
       {:display "inline-block"
        :vertical-align "middle"
        :margin-left "0.35rem"}]

      [:.explanation
       {:margin-left "1.5rem"
        :margin-top "0.25em"}
       [:p
        {:display "inline"
         :margin-right "0.25em"}]]]]]

   [:button
    {:font-size "1.25em"
     :padding "1rem"
     :background braid-color
     :border "none"
     :color "white"
     :border-radius border-radius
     :text-transform "uppercase"
     :white-space "nowrap"
     :letter-spacing "0.05em"
     :display "inline-block"}

    [:&::after
     {:content "\" ▶\""}]

    [:&:focus
     {:outline "none"}]]])

(defn style-view []
  [:style
   {:type "text/css"
    :dangerouslySetInnerHTML
    {:__html
     (css {:auto-prefix #{:transition
                          :flex-direction
                          :flex-shrink
                          :align-items
                          :animation
                          :flex-grow}
           :vendors ["webkit"]}

          (at-import "https://fonts.googleapis.com/css?family=Open+Sans:400,300,400italic,700")
          [:html
           {:background "#f3f3f3"}]

          [:body
           {:height "100vh"
            :font-family "Open Sans"
            :max-width "23em"
            :margin "0 auto"
            :line-height 1.5
            :background "white"
            :padding [[0 "5rem"]]
            }]

          (form-styles))}}])

(defn header-view []
  [:h1 "Braid"])

(defn form-view []
  [:form.register
   [header-view]

   [:div.option.group-name
    [:label
     [:h2 "Group Name"]
     [:input {:type "text"
              ;:value "" ; TODO guess from email
              :placeholder "Team Awesome"
              :auto-focus true}]
     [:div.explanation
      [:p "Your group's name will show up in menus and headings. It doesn't need to be formal."]]]]

   [:div.option.group-url
    [:label
     [:h2 "Group URL"]
     [:div.domain
      [:input {:type "text"
              ;:value "" ; TODO guess from email
               :placeholder "awesome"}]
      [:span ".braid.chat"]]
     [:div.explanation
      [:p "Pick something short and recognizeable."]
      [:p "Lowercase letters, numbers and dashes only."]]]]

   [:div.option.group-type
    [:h2 "Group Type"]
    [:label
     [:input {:type "radio" :name "type"}]
     [:span "Public Group"]
     [:div.explanation
      [:p "Anyone can find and join your group through the Braid Group Directory."]
      [:p "Unlimited everything. Free forever."]]]
    [:label
     [:input {:type "radio" :name "type" :default-checked true}]
     [:span "Private Group"]
     [:div.explanation
      [:p "Invite-only and hidden from the Braid Group Directory."]
      [:p "Free to evaluate, then pay-what-you-want."]]]]

   [:button "Create your group"]])

(defn app-view []
  [:div.app
   [style-view]
   [form-view]])
