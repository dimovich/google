
# Google API for [Roll](https://github.com/dimovich/roll)

<br>

## [Clojure CLI/deps.edn](https://clojure.org/guides/getting_started)

``` clojure
dimovich/roll {:git/url "https://github.com/dimovich/google"
               :sha "d0410bd91472ca474aa134522e988f01d2d15a93"}
```


## Usage


``` clojure
(ns my.app
  (:require [roll.google :as google]))


(google/init "credentials.edn")


(def sheet-id "< sheet id >")


(google/sheet-append sheet-id [["some" "data"]
                               ["more" "data"]])


(google/sheet-values sheet-id)


(google/send-message
 (->>
  ["Content-Type: text/plain;"
   "To: some@mail.com"
   "From: my@gmail.com"
   "Subject: ..."
   "some message"]
     
  (interpose "\n")
  (apply str)))

```


## Usage with Integrant

``` clojure
{:roll/google "credentials.edn"}
```


## Optaining Google Credentials

Create a new project in [Google API
console](https://console.developers.google.com/), and create new
`OAuth client ID` credentials. Use the following code with your
`client-id` and `client-secret`:

``` clojure
;; Google OAuth2 access token generation
;;


(require '[clj-oauth2.client :as oauth2])


(def credentials
  {:creds
   {:redirect-uri "http://localhost",
    :grant-type "authorization_code",
    :client-id ",,,,,",
    :client-secret ",,,,,",
    :authorization-uri "https://accounts.google.com/o/oauth2/auth",
    :access-query-param :access_token,
    :access-token-uri "https://accounts.google.com/o/oauth2/token",
    :scope
    ["https://www.googleapis.com/auth/gmail.send"
     "https://www.googleapis.com/auth/spreadsheets"],
    :access-type "online",
    :approval_prompt ""}})



;; get request link

(def auth-req
  (oauth2/make-auth-request (:creds credentials)))


;; navigate to the link inside auth-req and get code


(def code "<... insert code ...>")


;; get token
  
(def token (oauth2/get-access-token
            (:creds credentials)
            {:code code}
            auth-req))



;; save credentials

(->> (assoc credentials :token token)
     (spit "credentials.edn"))


```
