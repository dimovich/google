(ns roll.google
  (:require [taoensso.timbre :refer [info]]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [integrant.core :as ig]
            [clj-oauth2.client :as oauth2]
            [jsonista.core :as jsonista]
            [looper.client :as looper]
            [linked.core :as linked]
            [clojure.java.shell :as sh]))



(defonce credentials* (atom {}))



(defn keywordize [s]
  (-> (string/lower-case s)
      (string/replace #"_| " "-")
      keyword))


(def json-mapper
  (jsonista/object-mapper
   {:decode-key-fn keywordize}))


(defn- decode-json [s]
  (some-> s (jsonista/read-value json-mapper)))



(defn- backup [path]
  (sh/sh "mv" path (str path ".1")))



(defn- refresh-access-token
  [refresh-token {:keys [client-id client-secret access-token-uri]}]
  (let [req (looper/post
             access-token-uri
             {:form-params
              {:client_id client-id
               :client_secret client-secret
               :refresh_token refresh-token
               :grant_type "refresh_token"}})]
    
    (when (= (:status req) 200)
      (decode-json (:body req)))))




(defn- with-auth [f & args]
  (try
    (apply f (:token @credentials*) args)
    (catch Exception ex
      (case (:status (ex-data ex))
        ;; expired token
        401 (let [path (:path @credentials*)]
              (info "refreshing token...")
              ;; save old
              (backup path)
              ;; get new
              (->> (refresh-access-token
                    (get-in @credentials* [:token :params :refresh_token])
                    (:creds @credentials*))
                   (swap! credentials* update :token merge)
                   ;; save new
                   (spit path))
              ;; rerun
              (apply f (:token @credentials*) args))
         
        (throw ex)))))




(defn- send-message' [access-token message]
  (oauth2/post
   "https://www.googleapis.com/upload/gmail/v1/users/me/messages/send"
   {:oauth2 access-token
    :content-type "message/rfc822"
    :body message}))



(defn send-message
  "Send RFC822 compliant message."
  [message]
  (with-auth send-message' message))





(defn- insert-message' [access-token message]
  (oauth2/post
   "https://www.googleapis.com/upload/gmail/v1/users/me/messages"
   {:oauth2 access-token
    :content-type "message/rfc822"
    :body message}))



(defn insert-message [message]
  "Insert RFC822 compliant message into inbox."
  (with-auth insert-message' message))



;; https://developers.google.com/sheets/api/samples/writing#append_values

(defn- sheet-append' [access-token sheet-id data]
  (oauth2/post
   (str "https://sheets.googleapis.com/v4/spreadsheets/" sheet-id "/values/A1:B1:append")
   {:oauth2 access-token
    :query-params {"valueInputOption" "RAW"
                   "insertDataOption" "OVERWRITE"
                   ;;"includeValuesInResponse" true
                   }
    :body (-> { ;;"range" range
               "majorDimension" "ROWS"
               "values" data}
              (jsonista/write-value-as-string))}))



(defn sheet-append
  "`(sheet-append sheet-id [[\"hello\" \"my\"]])`"
  [sheet-id data]
  (with-auth sheet-append' sheet-id data))




(defn- sheet' [access-token sheet-id]
  (->>
   (oauth2/get
    (str "https://sheets.googleapis.com/v4/spreadsheets/" sheet-id)
    {:oauth2 access-token})
   :body
   decode-json))


(defn sheet
  "Get sheet properties."
  [sheet-id]
  (with-auth sheet' sheet-id))



(defn- num->col
  "Convert number to A1 format."
  [n]
  (loop [n n s ""]
    (let [n (dec n)
          c (mod n 26)
          s (str (char (+ 65 c)) s)
          n (/ (- n c) 26)]
      (if (< 0 n)
        (recur n s)
        s))))



(defn sheet-range
  "Returns data range in A1 notation."
  [sheet-id & [{:keys [cols rows]}]]
  (let [props (->> (sheet sheet-id)
                   :sheets first
                   :properties :gridproperties)
        colstr (num->col (or cols (:columncount props)))
        rows (or rows (:rowcount props))]
    (str "A1:" colstr rows)))





(defn- sheet-values' [access-token sheet-id range]
  (->>
   (oauth2/get
    (str "https://sheets.googleapis.com/v4/spreadsheets/" sheet-id "/values/" range)
    {:oauth2 access-token})
   :body
   decode-json
   :values))


(defn sheet-values [sheet-id range]
  (with-auth sheet-values' sheet-id range))



(defn init [credentials-path]
  (-> (slurp credentials-path)
      edn/read-string
      (assoc :path credentials-path)
      (->> (reset! credentials*))))



(defmethod ig/init-key :roll/google [_ credentials-path]
  (info "starting roll/google..." credentials-path)
  (init credentials-path))



;;(defmethod ig/halt-key! :roll/google [_ _])






(comment
  
  
  (def mail-template
    (->>
     ["Content-Type: text/plain;"
      "To: {{TO}}"
      "From: {{FROM}}"
      "Subject: {{SUBJECT}}"
      "{{MESSAGE}}"]
     
     (interpose "\n")
     (apply str)))
  
  
  
  (roll.google/send-message message)
  



  

  ;; Google OAuth2 access token generation
  ;;


  (def credentials
    {:creds
     {:redirect-uri "http://localhost",
      :grant-type "authorization_code",
      :client-id ",,,",
      :client-secret ",,,",
      :authorization-uri "https://accounts.google.com/o/oauth2/auth",
      :access-query-param :access_token,
      :access-token-uri "https://accounts.google.com/o/oauth2/token",
      :scope
      ["https://www.googleapis.com/auth/gmail.send"
       "https://www.googleapis.com/auth/spreadsheets"],
      :access-type "online",
      :approval_prompt ""}})



  ;; run this

  (def auth-req
    (oauth2/make-auth-request (:creds credentials)))

  ;; navigate to the link inside auth-req and get code


  (def code "<... insert code ...>")


  ;; get token
  
  (oauth2/get-access-token (:creds credentials)
                           {:code code}
                           auth-req)


  ;; update token in credentials file

  )

