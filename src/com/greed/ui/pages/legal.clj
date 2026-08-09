(ns com.greed.ui.pages.legal)

(def last-updated "2 August 2026")

(defn- badge [label]
  [:div {:class "inline-flex items-center gap-2 px-3 py-1.5 mb-4 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-full"}
   [:span {:class "w-1.5 h-1.5 bg-emerald-500 rounded-full"}]
   label])

(defn- page-header [label title intro]
  [:div {:class "py-16 lg:py-20"}
   [:div {:class "max-w-3xl mx-auto"}
    (badge label)
    [:h1 {:class "text-4xl font-bold text-zinc-900 lg:text-5xl"} title]
    [:p {:class "mt-4 text-zinc-500 leading-relaxed"} intro]
    [:p {:class "mt-3 text-sm text-zinc-400"} "Last updated " [:span {:class "font-medium text-zinc-500"} last-updated]]]])

(defn- section [title & children]
  [:section {:class "mt-10"}
   [:h2 {:class "text-xl font-bold text-zinc-900"} title]
   (for [child children] [:div {:class "mt-3 text-zinc-500 leading-relaxed"} child])])

(defn- bullet [& items]
  (for [item items]
    [:li {:class "mt-2 text-zinc-500 leading-relaxed"} item]))

(defn- legal-page [& children]
  [:div {:class "container mx-auto px-6 pb-24 lg:pb-32"}
   [:div {:class "max-w-3xl mx-auto"}
    children]])

(defn privacy-page [_]
  (legal-page
   (page-header
    "Privacy Policy"
    "How we handle your information."
    "A short, plain-language summary of what Greed knows about you, what we do with it, and the choices you have.")
   (section
    "The short version"
    [:p "We only collect the information we genuinely need to run the service. We never sell your data. We don't show you ads. And you can ask us to delete your data at any time."])
   (section
    "What we know about you"
    [:p "When you create an account you give us some details so we can set up and run your plan:"]
    [:ul {:class "list-disc pl-5 mt-3"}
     (bullet
      "Your email address, so you can sign in."
      "Your first and last name and age, so we can show you relevant information."
      "A password, which is stored securely and never in plain text.")]
    [:p "As you use Greed, you choose what financial information to add. This can include:"]
    [:ul {:class "list-disc pl-5 mt-3"}
     (bullet
      "Your salary, bank and card type, and payday."
      "Your budget items, goals, and savings amounts."
      "Tax details such as medical aid contributions and retirement annuity contributions."
      "Any calendar events you add.")]
    [:p "This information belongs to you. It's stored so we can calculate your numbers and show you your plan — not so we can profile you."])
   (section
    "How we keep it secure"
    [:p "Your data is stored safely, your account is protected, and access is limited to what's needed to keep the service running. Your password is never stored in readable form, and only you (and the team operating the service, where necessary) can see the information in your account."])
   (section
    "Cookies and technical data"
    [:p "We use a single cookie so you can stay signed in while you use the service. We may also keep basic technical logs (like error records) to keep the service secure and reliable. We don't use advertising trackers or sell data to third parties."])
   (section
    "Sharing your data"
    [:p "We don't sell, rent, or share your personal or financial information with third parties for marketing. We only disclose information where we're legally required to, or where a provider we use needs it to operate the service for you."])
   (section
    "Deleting your data"
    [:p "You can stop using Greed at any time. If you want your account and its information removed, ask us and we'll take care of it — just contact us using the details below."])
   (section
    "Changes to this policy"
    [:p "If we change how we handle your data, we'll update this page and the date above. We'll always be upfront about it."])
   (section
    "Contact"
    [:p "Questions about your data or this policy? Get in touch at "
     [:a {:href "mailto:support@mygreed.co.za" :class "text-emerald-600 font-medium hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:text-emerald-700"} "support@mygreed.co.za"]
     "."])))

(defn terms-page [_]
  (legal-page
   (page-header
    "Terms & Conditions"
    "The rules of using Greed."
    "What you agree to when you create an account and use the service.")
   (section
    "The service"
    [:p "Greed is a personal finance wellbeing platform. It helps you organise your salary, spending, savings and goals in one place. By creating an account, you agree to these terms."])
   (section
    "Your account"
    [:p "You're responsible for keeping your sign-in details safe and for what happens under your account. Please use a strong password and let us know if you think your account has been compromised."])
   (section
    "Your information"
    [:p "You agree to provide information that is accurate and that belongs to you. You can add, correct, or remove your information at any time, and you can ask us to delete your account and its data."])
   (section
    "How the service works"
    [:p "The calculations and figures on Greed are provided as general guidance to help you understand and organise your money. They are not financial, legal, or tax advice, and they shouldn't be relied on as a substitute for advice from a qualified professional."])
   (section
    "Acceptable use"
    [:p "Please use the service responsibly. Don't misuse it, try to gain unauthorised access to it, or use it in any way that breaks the law."])
   (section
    "Availability"
    [:p "We work to keep Greed available and accurate, but we can't guarantee it will always be up, bug-free, or that every figure will be right in every situation. The service is provided \"as is\" and \"as available\"."])
   (section
    "Our content"
    [:p "The design, content, and branding of Greed belong to us. You get a personal licence to use the service for your own finances, but you can't copy or reuse it commercially without permission."])
   (section
    "Limits of liability"
    [:p "To the fullest extent allowed by law, we're not liable for any loss or damage arising from your use of the service or from relying on the information it provides. Nothing in these terms limits rights you have under South African law that can't be limited."])
   (section
    "Changes to these terms"
    [:p "We may update these terms from time to time. If we make a meaningful change, we'll update the date above and, where possible, let you know. Continuing to use Greed means you accept the updated terms."])
   (section
    "Governing law"
    [:p "These terms are governed by the laws of the Republic of South Africa."])
   (section
    "Contact"
    [:p "Questions about these terms? Get in touch at "
     [:a {:href "mailto:support@mygreed.co.za" :class "text-emerald-600 font-medium hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 active:text-emerald-700"} "support@mygreed.co.za"]
     "."])))
