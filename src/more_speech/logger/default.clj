(ns more-speech.logger.default
  (:import (java.text SimpleDateFormat)
           (java.util Date)))

(defn make-default-logger []
  {::type ::default})

(defn get-now []
  (System/currentTimeMillis))

(defn format-time []
  (let [time (get-now)
        date (Date. (long time))]
    (.format (SimpleDateFormat. "MM/dd/yy HH:mm:ss") date))
  )

(def instance (atom (make-default-logger)))
(def log-level (atom 1))

(defn log-agent-error-handler [log-agent e]
  (prn 'log-agent-error e)
  (restart-agent log-agent nil))

(def log-agent (agent nil))
(set-error-handler! log-agent log-agent-error-handler)

(defn set-level [level]
  (reset! log-level level))

(defn get-level []
  @log-level)

(defmulti log-msg (fn [logger _time _level _message] (::type logger)))

(defn log [level message]
  (when (and @instance (<= level @log-level))
    (log-msg @instance (format-time) level message)))

(defn log-pr [level & args]
  (log level (apply pr-str args)))

(defn do-log [_ time level message]
  (printf "L%d %s | %s\n" level time message)
  (flush))

(defmethod log-msg ::default [_logger time level message]
  (send-off log-agent do-log time level message)
  )



