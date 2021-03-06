
# Google API for Clojure
(Works nicely with [Roll](https://github.com/dimovich/roll))

<br>

## [Clojure CLI/deps.edn](https://clojure.org/guides/getting_started)

``` clojure
roll/google {:git/url "https://github.com/dimovich/google"
             :sha "7bd7facaea72ae39c61e3593a680fa7db84d4ce3"}
```

<br>

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
  ["To: some@mail.com"
   "Subject: ..."
   "some message"]
     
  (interpose "\n")
  (apply str)))


;; You can easily add more api endpoints as needed.

```

<br>

## Usage with Integrant

``` clojure
{:roll/google "credentials.edn"}
```

<br>

## Optaining Google Credentials

Create a new project in [Google API
console](https://console.developers.google.com/), and create new
`OAuth client ID` credentials. Run the following code at the REPL using
your `client-id` and `client-secret`:

``` clojure
;; Google OAuth2 access token generation
;;


(require '[clj-oauth2.client :as oauth2])


(def credentials
  {:creds
   {:client-id ",,,,,",
    :client-secret ",,,,,",
    :scope ["https://www.googleapis.com/auth/gmail.send"
            "https://www.googleapis.com/auth/spreadsheets"],
    :redirect-uri "http://localhost",
    :grant-type "authorization_code",
    :authorization-uri "https://accounts.google.com/o/oauth2/auth",
    :access-query-param :access_token,
    :access-token-uri "https://accounts.google.com/o/oauth2/token",
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
