(ns com.greed.ui.pages.legal)

(def last-updated "22 November 2026")

(def operator
  "Who runs greed, as South African law asks a website that sells online to
   show (ECTA s43). Fill these in before taking live payments; the
   \"Who we are\" sections only show once :name is set."
  {:name nil                ; e.g. "Ngunitek (Pty) Ltd"
   :registration nil        ; CIPC registration number
   :address nil             ; physical address for legal notices
   :email "support@mygreed.co.za"})

(def ^:private mail-link
  [:a {:href "mailto:support@mygreed.co.za" :class "font-medium text-zinc-900 underline underline-offset-4 hover:text-zinc-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 rounded-sm"} "support@mygreed.co.za"])

(defn- who-we-are []
  (when-let [nm (:name operator)]
    [:p (str "greed is run by " nm
             (when-let [r (:registration operator)] (str " (registration number " r ")"))
             (when-let [a (:address operator)] (str ", " a))
             ". You can reach us at ")
     mail-link "."]))

(defn- page-header [title intro]
  [:div {:class "py-16 lg:py-20"}
   [:div {:class "max-w-3xl mx-auto"}
    [:h1 {:class "font-semibold text-zinc-900 [font-size:clamp(40px,5vw,64px)] leading-[1.05] tracking-[-0.04em]"} title]
    [:p {:class "mt-6 max-w-[60ch] text-xl text-zinc-700 leading-relaxed"} intro]
    [:p {:class "mt-3 text-sm text-zinc-600"} "Last updated " [:span {:class "font-medium text-zinc-900"} last-updated]]]])

(defn- section [title & children]
  [:section {:class "mt-10"}
   [:h2 {:class "text-2xl font-semibold tracking-[-0.02em] text-zinc-900"} title]
   (for [child children] [:div {:class "mt-3 text-[17px] text-zinc-800 leading-relaxed"} child])])

(defn- bullet [& items]
  (for [item items]
    [:li {:class "mt-2 text-[17px] text-zinc-800 leading-relaxed"} item]))

(defn- legal-page [& children]
  [:div {:class "container mx-auto px-6 pb-24 lg:pb-32"}
   [:div {:class "max-w-3xl mx-auto"}
    children]])

(defn privacy-page [_]
  (legal-page
   (page-header
    "How we handle your information."
    "A plain-language summary of what greed knows about you, what we do with it, who helps us run it, and the choices you have.")
   (section
    "The short version"
    [:p "We only collect what we need to run the service. We never sell your data and we don't show you ads. Bank statements are read and then let go: we keep the totals, never the file. You can ask us to delete your data at any time."])
   (when (:name operator) (section "Who we are" (who-we-are)))
   (section
    "What we know about you"
    [:p "When you create an account you give us some details so we can set up and run your plan:"]
    [:ul {:class "list-disc pl-5 mt-3"}
     (bullet
      "Your email address, so you can sign in and receive emails about your account."
      "Your first and last name, and your age if you sign up as a person, so we can show you relevant tax information."
      "A password, which is stored securely and never in plain text.")]
    [:p "As you use greed, you choose what financial information to add. This can include:"]
    [:ul {:class "list-disc pl-5 mt-3"}
     (bullet
      "Your salary, bank and account type, and payday."
      "Your budget items, goals, savings amounts and calendar events."
      "Tax details such as medical aid and retirement annuity contributions."
      "Businesses you run: their name, type, VAT and tax settings, budget, and the people you invite.")]
    [:p "This information belongs to you. It's stored so we can calculate your numbers and show you your plan, not so we can profile you."])
   (section
    "Bank statements"
    [:p "When you upload a PDF statement, greed reads it in memory and saves only monthly totals: money in and out, totals per category and per payee (with dates and account numbers taken out), and bank fees. The file itself is never written to disk or kept. You can delete a month's totals at any time on the Banking page."])
   (section
    "Businesses and the people you invite"
    [:p "A business's budget, statements and tax figures are kept apart from your personal ones. The people you invite to a business can see and work with that business's information, but never your personal budget or statements. An owner of the business decides who is invited and can remove people."])
   (section
    "Paid plans and payments"
    [:p "Paid plans are billed by Paystack, our payment provider. You enter your card details on Paystack's own checkout page: greed never sees or stores your card number."]
    [:p "From Paystack we keep only what we need to run your plan: your Paystack customer and subscription references, your plan, its status and renewal date, and a record of each payment event (what happened and when, with its reference). We keep payment records for as long as South African tax law requires, which can be longer than your account."])
   (section
    "Who helps us run greed"
    [:p "We use a small number of service providers, who may only use your information to provide their service to us:"]
    [:ul {:class "list-disc pl-5 mt-3"}
     (bullet
      "Paystack, to take payments for paid plans."
      "MailerSend, to send account emails such as sign-in links, invitations and plan reminders."
      "Google reCAPTCHA, to keep bots out of sign-up and sign-in, and Google Analytics, to count visits and see which pages are used."
      "DigitalOcean, which hosts our servers and database.")]
    [:p "Some of these providers process information outside South Africa. Where they do, we rely on them to protect it to a standard comparable to South African law, as the Protection of Personal Information Act (POPIA) requires."])
   (section
    "Cookies and technical data"
    [:p "We use one cookie to keep you signed in. Google Analytics and reCAPTCHA set their own cookies. We also keep basic technical logs, such as error records, to keep the service secure and reliable. We don't use advertising trackers."])
   (section
    "How we keep it secure"
    [:p "Your data is stored safely, your account is protected, and access is limited to what's needed to keep the service running. Your password is never stored in readable form, and only you (and the team operating the service, where necessary) can see the information in your account."])
   (section
    "Your rights"
    [:p "Under POPIA you can ask what information we hold about you, ask us to correct it, or ask us to delete it. You can also object to how we use it, and complain to the Information Regulator if you're not happy with how we respond. To do any of this, email us."])
   (section
    "Deleting your data"
    [:p "You can stop using greed at any time. Ask us and we'll delete your account and its information, and stop any paid plan from renewing. Payment records we must keep by law are kept, without your name or email address."])
   (section
    "Changes to this policy"
    [:p "If we change how we handle your data, we'll update this page and the date above, and tell you by email when the change matters. We'll always be upfront about it."])
   (section
    "Contact"
    [:p "Questions about your data or this policy? Get in touch at " mail-link "."])))

(defn terms-page [_]
  (legal-page
   (page-header
    "The rules of using greed."
    "What you agree to when you create an account, use the service, and pay for a plan.")
   (section
    "The service"
    [:p "greed helps you organise your money: your salary, budget, spending from bank statements, savings, goals and tax, and the same for businesses you run. By creating an account, you agree to these terms."])
   (when (:name operator) (section "Who we are" (who-we-are)))
   (section
    "Your account"
    [:p "You're responsible for keeping your sign-in details safe and for what happens under your account. Please use a strong password and let us know if you think your account has been compromised."])
   (section
    "Your information"
    [:p "You agree to provide information that is accurate and that belongs to you, or that you're allowed to use, such as your own bank statements or those of a business you run. You can add, correct, or remove your information at any time, and you can ask us to delete your account and its data."])
   (section
    "Businesses and teams"
    [:p "If you add a business, you confirm you're allowed to manage its finances. If you invite people to it, they can see and change that business's information, and you're responsible for who you invite. An owner can change people's roles or remove them at any time."])
   (section
    "Plans and the free trial"
    [:p "greed has a free plan and paid plans (Premium and Business). What each plan includes, and its price, is shown on the "
     [:a {:href "/pricing" :class "font-medium text-zinc-900 underline underline-offset-4 hover:text-zinc-600"} "Pricing"]
     " page. Prices are in South African rand."]
    [:p "New accounts get a free trial of everything for 14 days, once per person, with no card needed. When the trial ends you move to the free plan unless you choose a paid one. Nothing you added is deleted."]
    [:p "On the free plan, you can read one month of bank statements. Months you have already read stay visible if you move to the free plan; reading more, and using businesses, needs a paid plan."])
   (section
    "Paying, renewing and cancelling"
    [:ul {:class "list-disc pl-5 mt-3"}
     (bullet
      "Paid plans are charged in advance, monthly or yearly, to the card you give Paystack, and renew automatically at the end of each period until you cancel."
      "You can cancel at any time from Manage billing on your Plan page. You keep your plan until the end of the period you've paid for, and it doesn't renew after that."
      "If a renewal payment fails, we'll let you know by email. Your plan stays on while you update your card; if payment still can't be taken, you move to the free plan."
      "If we change a plan's price, we'll tell you by email at least 30 days before, and the new price applies from your next renewal after that. If you don't want to continue, you can cancel before then.")])
   (section
    "Refunds"
    [:p "If you change your mind, ask us within 7 days of your first payment for a plan and we'll refund it in full. After that, payments for a period that has started aren't refunded, but cancelling stops any further charges. If we charge you in error, we'll refund the amount. Refunds go back to the card you paid with."]
    [:p "Nothing here limits rights you have under the Consumer Protection Act or the Electronic Communications and Transactions Act."])
   (section
    "How the service works"
    [:p "The calculations and figures on greed, including income tax, VAT and company tax, are estimates to help you understand and plan your money. They are not financial, legal, or tax advice, and they shouldn't be relied on as a substitute for advice from a registered tax practitioner or financial adviser. You remain responsible for what you submit to SARS."])
   (section
    "Acceptable use"
    [:p "Please use the service responsibly. Don't misuse it, try to gain unauthorised access to it, or use it in any way that breaks the law. If you do, we may suspend your account or end your plan, and if we end a paid plan for this reason we won't refund it."])
   (section
    "Availability"
    [:p "We work to keep greed available and accurate, but we can't guarantee it will always be up, bug-free, or that every figure will be right in every situation. The service is provided \"as is\" and \"as available\"."])
   (section
    "Our content"
    [:p "The design, content, and branding of greed belong to us. You get a licence to use the service for your own finances and those of businesses you run, but you can't copy or reuse it commercially without permission."])
   (section
    "Limits of liability"
    [:p "To the fullest extent allowed by law, we're not liable for any loss or damage arising from your use of the service or from relying on the information it provides. Nothing in these terms limits rights you have under South African law that can't be limited."])
   (section
    "Changes to these terms"
    [:p "We may update these terms from time to time. If we make a meaningful change, we'll update the date above and let you know by email. Continuing to use greed means you accept the updated terms; if you don't, you can cancel your plan and ask us to delete your account."])
   (section
    "Governing law"
    [:p "These terms are governed by the laws of the Republic of South Africa."])
   (section
    "Contact"
    [:p "Questions about these terms? Get in touch at " mail-link "."])))
