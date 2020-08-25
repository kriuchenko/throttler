# throttler
Web request throttling service

Load testing
Run
mvn gatling:test

Current test configured for 1000 users during 10 seconds doing 10 requests each with delay 1 second

Sample local results:<br>
---- Global Information --------------------------------------------------------
> request count                                       1000 (OK=1000   KO=0     )
> min response time                                      0 (OK=0      KO=-     )
> max response time                                     18 (OK=18     KO=-     )
> mean response time                                     1 (OK=1      KO=-     )
> std deviation                                          2 (OK=2      KO=-     )
> response time 50th percentile                          1 (OK=1      KO=-     )
> response time 75th percentile                          1 (OK=1      KO=-     )
> response time 95th percentile                          3 (OK=3      KO=-     )
> response time 99th percentile                          7 (OK=7      KO=-     )
> mean requests/sec                                     50 (OK=50     KO=-     )
