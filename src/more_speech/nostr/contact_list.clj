(ns more-speech.nostr.contact-list
  (:require [more-speech.nostr.util :as util]
            [more-speech.ui.swing.ui-context :refer :all]
            [more-speech.db.gateway :as gateway]
            [more-speech.config :refer [get-db]]))


(defn make-contact-from-tag [[_p pubkey relay petname]]
  (try
    {:pubkey (util/hex-string->num pubkey) :relay relay :petname petname}
    (catch Exception e
      (prn 'make-contact-from-tag 'exception [pubkey relay petname] (.getMessage e))
      nil))
  )

(defn unpack-contact-list-event [event]
  (let [pubkey (:pubkey event)
        tags (:tags event)
        ptags (filter #(= :p (first %)) tags)
        contacts (map make-contact-from-tag ptags)
        contacts (remove nil? contacts)]
    [pubkey contacts]))

(defn process-contact-list [db event]
  (let [[pubkey contacts] (unpack-contact-list-event event)]
    (when (seq contacts)
      (gateway/add-contacts db pubkey contacts))))

(defn is-trusted? [candidate-pubkey]
  (let [event-state @(:event-context @ui-context)
        my-pubkey (:pubkey event-state)
        contact-lists (:contact-lists event-state)
        my-contacts (get contact-lists my-pubkey)
        my-contact-pubkeys (set (map :pubkey my-contacts))]
    (or (= candidate-pubkey my-pubkey)
        (contains? my-contact-pubkeys candidate-pubkey))))

(defn trusted-by-contact [candidate-pubkey]
  (let [event-state @(:event-context @ui-context)
        my-pubkey (:pubkey event-state)
        contact-lists (:contact-lists event-state)
        my-contacts (get contact-lists my-pubkey)
        my-contact-ids (map :pubkey my-contacts)]
    (loop [my-contact-ids my-contact-ids]
      (if (empty? my-contact-ids)
        nil
        (let [my-contact (first my-contact-ids)
              his-contacts (set (map :pubkey (get contact-lists my-contact)))]
          (if (contains? his-contacts candidate-pubkey)
            my-contact
            (recur (rest my-contact-ids)))))))
  )

(defn get-petname [his-pubkey]
  (let [event-state @(:event-context @ui-context)
        my-pubkey (:pubkey event-state)
        contact-lists (:contact-lists event-state)
        my-contacts (get contact-lists my-pubkey)
        his-entry (first (filter #(= his-pubkey (:pubkey %)) my-contacts))]
    (:petname his-entry)))

(defn get-pubkey-from-petname [petname]
  (gateway/get-id-from-petname (get-db) (get-mem :pubkey) petname))



