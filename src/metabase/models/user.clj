(ns metabase.models.user
  (:require [korma.core :refer :all]
            [metabase.db :refer :all]
            [metabase.models.org-perm :refer [OrgPerm]]))

(defentity User
  (table :core_user)
  (has-many OrgPerm {:fk :user_id}))

;; fields to return for Users other `*than current-user*`
(defmethod default-fields User [_]
  [:id
   :email
   :date_joined
   :first_name
   :last_name
   :last_login
   :is_superuser])

(def current-user-fields
  "The fields we should return for `*current-user*` (used by `metabase.middleware.current-user`)"
  (concat (default-fields User)
          [:is_active
           :is_staff])) ; but not `password` !

(defn user-perms-for-org
  "Return the permissions level User with USER-ID has for Org with ORG-ID.
   nil      -> no permissions
   :default -> default permissions
   :admin   -> admin permissions"
  [user-id org-id]
  (let [{:keys [admin] :as op} (sel :one [OrgPerm :admin] :user_id user-id :organization_id org-id)]
    (when op
      (if admin :admin :default))))

(defmethod post-select User [_ {:keys [id] :as user}]
  (-> user
      (assoc :org_perms (sel-fn :many OrgPerm :user_id id)
             :perms-for-org (memoize (partial user-perms-for-org id))
             :common_name (str (:first_name user) " " (:last_name user)))))
