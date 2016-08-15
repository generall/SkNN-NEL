# SkNN-NER
Implementation of named entity disambiguation and linking with usage of SkNN algorithm

DBPedia categories hierarchy used as words representation

# NER resolving example

Training set:
```
(Andrei Tarkovsky) (filmed) (Stalker)
(Edward Quayle) (command) (SS Mona)
```
Pre-resolved concepts: 

Andrei Tarkovsky = http://dbpedia.org/resource/Andrei_Tarkovsky

Edward Quayle = http://dbpedia.org/resource/Edward_Quayle_(sea_captain)

Stalker = http://dbpedia.org/resource/Stalker_(1979_film)

SS Mona = http://dbpedia.org/resource/SS_Mona_(1832)

Test set:
```
(James Cameron) (made) (Titanic)
(Edward Smith) (ruled) (Titanic)
```
James Cameron = http://dbpedia.org/resource/James_Cameron

Edward Smith = http://dbpedia.org/resource/Edward_Smith_(sea_captain)

Titanic = DISAMBIGUATION: (http://dbpedia.org/resource/RMS_Titanic | http://dbpedia.org/resource/Titanic_(1997_film))

## Test result:

Titanic successfully resolved in test set

