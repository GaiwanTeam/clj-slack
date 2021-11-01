(ns co.gaiwan.slack.test-data.markdown
  "Markdown sample values

  Used for unit tests and UI (visual) testing.")

(def bold-italic-del
  "Show some basic inline formatting"
  "This is a rich text testing:\n1. *bold face*\n2. _italics_\n3. ~strike~\n\norder test\na\nb")

(def user-references
  "A message with some user references"
  "Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR>")

(def complicated-message
  "A message that has a bit of everything"
  "*Hey everyone, we\u2019re so excited to be here for DevOps Enterprise Summit talking about*\n:arrow_right: _*Be sure to visit our booth <https://doesvirtual.com/teamform>*_ \n:tv: _*Or join us anytime on Zoom -\u00a0<https://bit.ly/3iIdX1X>*_\n:mega: _*Schedule a private demo - <https://teamform.co/demo>*_\n:gift: _*Register for giveaway (1x PS5 or XBox Series X, 1 x 50min chat with the authors of Team Topologies, 20x IT Rev Books) <https://www.teamform.co/does-giveaway>*_\n\n*We\u2019ve got a exciting week with a bunch of demos of TeamForm scheduled*\n:star: 11-11:15am PDT: TeamForm Live Demo: Managing Supply &amp; Demand at Scale - join @ <https://us02web.zoom.us/j/81956904920>\n:star: 12:45-1:00pm PDT: TeamForm Live Demo: Measuring Team Organising Principles - join @ <https://us02web.zoom.us/j/81956904920>\n:bar_chart: 3:45-4pm PDT: TeamForm Live Demo: Measuring Team Proficiency - join @ <https://us02web.zoom.us/j/81956904920>\n\nLater this week:\n:arrow_right: Register for our AMA with Authors of TeamTopologies <https://sched.co/ej42> with <@ULTTZCP7S> &amp; <@UBE001UAX>")
