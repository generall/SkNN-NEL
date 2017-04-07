$(function () {

  $body = $("body");

  $(document).on({
    ajaxStart: function() { $body.addClass("loading");    },
    ajaxStop: function() { $body.removeClass("loading"); }
  });

  function request(addr, data, type, isJson) {
    return $.ajax({
      type: type,
      url: addr,
      crossDomain: true,
      data: isJson ? JSON.stringify(data) : data,
      contentType: (isJson ? "application/json;" : "plain/text;") + " charset=utf-8",
      dataType: "json"
    });
  }

  function requestJson(addr, data){
    return request(addr, data, "POST", true);
  }

  function callNEL(sentence){
    var addr = "http://localhost:4567/analyze";
    var data = {
        s: sentence
    };
    return requestJson(addr, data);
  }

  function submitSentence(){
    console.log("submitSentence")
    var sentence = $("#sentence").val()
    callNEL(sentence).then( console.log )
  }


  $("#submitButton").on("click", submitSentence)

  var load = () => {
    addSent('PTH 7 first appeared on the 1928 Manitoba Highway Map as a short feeder route connecting Stonewall and Winnipeg.');
    addSent('When PTH 6 was opened to traffic in 1947, it incorporated a small portion of the original PTH 7.');
    addSent('That same year, a second leg of PTH 7 was opened connecting Stony Mountain to Teulon.');
    addSent('"And the Beat Goes On" is a 1980 single by the American music group the Whispers.');
    addSent('The song was their first of two number-one singles on the Soul chart, and their first Top 20 hit on the Billboard Hot 100, peaking at number 19.');
    addSent('In the 1920s, the Kurdish community in Azerbaijan was considerably diminished, when many of them moved to Armenia where Kurdish villages were created.');
    addSent('About the same time Azerbaijans Kurds had their own region called Red Kurdistan in the Lachin region, which was to the West of Karabakh.');
    addSent('In fact, Lachin with the principal towns Kalbajar, Kubatli and Zangelan and the administrative sub-divisions of Karakushlak, Koturli, Murad-Khanli and Kurd-Haji were mostly inhabited by Kurds.');
    addSent('He started his career in 1970 with BOC Gases; where he served for nearly 26 years, holding several senior positions.');
    addSent('In 1995, he was appointed executive director and Chief Executive of BTR Nylex (which later became Invensys plc).');
    addSent('From 1997 until his retirement in 2006, he served in BHP Billiton; firstly as President of BHP Petroleum, and latterly as Group President of BHP Billitons Energy business.');
    addSent('From 2006 till 2009, he was a senior advisor at Macquarie Bank Ltd.');
    addSent('Cup-o-Gold is a candy bar in the form of a chocolate cup with a marshmallow center and contains almonds and coconut.');
    addSent('It is similar to products such as Mallo Cups or Valomilk.');
    addSent('It was invented in the 1950s by the Hoffman Candy Company in Los Angeles and is now distributed by Los Angeles candy company Adams & Brooks.');
    addSent('It is available primarily on the West Coast but can also be bought online through the manufacturers website.');
    addSent('Mycerinopsis excavata is a species of beetle in the family Cerambycidae.');
    addSent('It was described by Breuning in 1948.');
    addSent('Born in Stratford in 1908, Black was the son of Maud Harriet (née Shalders) and Wilfred Alick Black, a solicitor.');
    addSent('At the 1932 national amateur athletics championships in Auckland, Black won the 440 yards title with a New Zealand record time of 48.');
    addSent('8 s, and finished second behind Allan Elliot in the 220 yards.');
    addSent('Born in San Francisco, California, Madera Uribe was ordained a priest for the Missionaries of the Holy Spirit on June 15, 1957.');
    addSent('On December 18, 1979, he was appointed coadjutor of the Roman Catholic Diocese of Fresno and was consecrated bishop on March 4, 1980.');
    addSent('He succeeded as bishop of the diocese on June 1, 1980.');
    addSent('Although elephantiasis nostras resembles the elephantiasis caused by helminths, it is not a filarial disease.');
    addSent('Instead, it is a complication of chronic lymphedema.');
    addSent('Both elephantiasis nostras and filarial elephantiasis are characterized by impaired lymphatic drainage, which results in excess fluid accumulation.');
    addSent('Born in Chile, Murúa moved to Argentina at the beginning of the fifties.');
    addSent('He studied architecture and fine arts before entering the film industry.');
    addSent('He worked primarily as an actor and appeared in over 80 films between 1949 and his death in 1995 although he also directed a handful of important films such as Shunko, Alias Gardelito and La Raulito, all with stories usually revolving around social topics.');
    addSent('In 1016, another campaign by Basil II was stopped by Krakra at Pernik after an unsuccessful 88-day Byzantine siege.');
    addSent('As the Byzantine-Bulgarian conflict continued, Krakra and Ivan Vladislav looked for Pecheneg support for a large-scale Bulgarian campaign against the Byzantines and initially persuaded the Pechenegs to collaborate in the winter of 1016–1017.');
    addSent('However, the Byzantine governor of Dorystolon learned about the plan and notified Basil II.');
    addSent('Upon hearing this, the Pechenegs declined to take part, effectively ruining the Bulgarian plans.');
    addSent('For a few years, relations between Cameroon and Nigeria have intensified over issues relating to their 1,600-kilometer land boundary, extending from the Lake Chad to the Bakassi peninsula, and boundary into the Gulf of Guinea.');
    addSent('The issues that are involved are rights over the oil-rich land and the fate of local populations.');
    addSent('For example, as Lake Chad dried up due to desertification, local populations relying on the lake for their water source have followed the receding waters, further blurring the boundary lines.');
    addSent('Tensions between the two countries escalated into military confrontation at the end of 1993 with the deployment of Nigerian military to the Bakassi peninsula.');
    addSent('The dispute was resolved with the Greentree agreement of 2006.');
    addSent('A growing number of Christians– a shocking amount, actually– are convinced that America’s glory days are over and that Christians are now a marginalized group on the verge of having all of their rights stripped away.');
    addSent('I recently saw an advertisement for a nation-wide Christian event prior to the election, and the advertisement boasted they would help Christians figure out what to do in a country that was growing hostile towards Christianity.');
    addSent('Elsewhere, right-wing politicians and religious talking-heads like Franklin Graham are trying to convince people that "religious liberty" is not only being threatened, but on the verge of disappearing.');
    addSent('In some corners, it’s all out panic.');
    addSent('Leaders are shouting it, and the simple-minded unquestionably believe it.');
    addSent('Except– and here’s the kicker– it’s not true.');
    addSent('Christians are not a marginalized minority in America, but the majority and the ruling class.');
    addSent('In fact, some polls show that around 83% of Americans are Christians.');
    addSent('That long line of U.S presidents stemming back to the founding of the nation?');
    addSent('Well, except for Abraham Lincoln and Andrew Johnson, all of them were professing Christians to varying degrees.');
    addSent('You know, the people who actually make the laws we live by in America?');
    addSent('Well, that group of people is actually 91.8% Christian.');
    addSent('And let’s not forget the Supreme Court, the body that decides which laws are constitutional and which ones are not– that’s predominantly stacked with Christians too, having two justices who are Jewish, and the rest entirely Christians.');
    addSent('If America were truly hostile towards Christians, that would be a massive indictment against Christians themselves— because America is near-entirely controlled by Christians.');
    addSent('The idea that America is hostile to Christians and that the liberty to practice Christianity is under attack is misguided at best, and a complete fabrication designed to control the fearful and ignorant at worst.');
    addSent('Like all distorted thinking, this idea that America is growing hostile towards Christians is rooted in a degree of truth– most broken thinking is.');
    addSent('However, here’s the part that’s true: America isn’t growing hostile towards Christians– it is growing hostile towards religious bullies, and there’s a big difference between those two things.');
    addSent('Few sane people give a hoot if one is a practicing Christian.');
    addSent('There’s no movement to banish churches and put them under government regulation like in China.');
    addSent('No one is stopping us from gathering together with other believers, from feeding the poor, or even from standing on the street corner with obnoxious banners that say "turn or burn."');
    addSent('Religious freedom and free speech is alive and well in America.');
    addSent('These freedoms aren’t just tolerated, but embraced.');
    addSent('What is not embraced, and what the majority of citizens (Christians citizens, mind you) are growing increasingly hostile towards, are fringe Christian extremists who are trying to institute their own version of sharia law that infringes on the rights and liberties of the rest of us.');
    addSent('There’s a massive difference between freedom to practice one’s religion in a pluralistic society where we all equally have that right, versus enshrining one’s extremist religious views in laws that are imposed on the rest of us.');
    addSent('There’s a big difference between saying that you want to be free and not forced to marry someone of the same sex, versus wanting to deny that right to someone else you don’t even know.');
    addSent('There’s a big difference between wanting the freedom to own a business and conduct commerce freely in the public square, versus demanding to run a business that discriminates and infringes on the basic rights and dignities of everyone else.');
    addSent('No one is trying to stop you from being a Christian.');
    addSent('The country is not growing hostile towards Christians.');
    addSent('It’s just growing hostile towards extremist, religious bullies, who are trying to hijack the nation and force everyone to live under their own set of morals and ethics.');
    addSent('Growing hostile towards that kind of nonsense is not the same thing as growing hostile towards Christianity.');
    addSent('It’s not even close.');
    addSent('Perhaps the most amusing aspect of this quest for "religious rights" is the sheer hypocrisy of it all.');
    addSent('And then, they turn from those discussions and do the very thing they condemned just moments before– they demand special rights, and demand that their religious code influence the laws that everyone else is governed by.');
    addSent('This is precisely the kind of thing that made Jesus throw up his hands and shout, "You hypocrites" over and over again in the Gospels.');
    addSent('If our friends on the religious right think we’re growing hostile, it’s because it’s true.');
    addSent('But no, it’s not because we’re growing hostile towards the practice of our own religion, or hostile towards religious liberty.');
    addSent('We’re just growing hostile towards hypocrites and religious bullies who aren’t content to just live their lives the way they please, but who instead seek to impose their extremist beliefs on the rest of us.');
    addSent('While I’m learning a new subject, I found out that it was a very valuable exercise to write a comprehensive guide about my studies.');
    addSent('So I’m now sharing my progress looking for useful feedback while helping others reach understanding faster.');
    addSent('The reader should already have some understanding of what is a neural network and in linear algebra before reading this.');
    addSent('I’ve been spending the last few days to understand what is sparse coding, Here are my results!');
  }

});


