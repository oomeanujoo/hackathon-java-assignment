# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

Honestly, I don't think there's one clear winner here, both ways have their place.

Using OpenAPI (like they did for Warehouse) is nice because the yaml file becomes the single
source of truth. Anyone can open it and see exactly what the API looks like without digging
through the code, and things like Swagger UI and the request/response classes get generated
from it automatically, so the docs can't really go out of date. It also forces you to keep the
API shape separate from your database model, which is a good habit. If someone changes a column
in the database later, it doesn't automatically break the API for everyone using it. The
downside is it's just more work day to day. You have to edit the yaml, regenerate the code, then
go update your implementation to match, which feels slow when you just want to make a small
change. It also means there are more classes floating around for basically the same "warehouse"
concept, which was a little confusing to me at first when going through the code.

The hand-coded way (Product and Store) is much quicker to write and easier to follow, especially
for a small project like this one. You just add a method and you're done, no extra build step.
But looking at the code, Product and Store return the database entity directly as the API
response. That feels risky to me, because now the database structure basically IS the public
API. If someone renames a column or adds something for storage reasons only, it can quietly
change what the API sends back too, without anyone noticing until it breaks something.

If I had to pick, it would depend on who is using the API. For something other teams or outside
systems depend on and expect to stay stable, like Warehouse probably is, I'd go with the OpenAPI
route even if it's a bit slower to build. For small internal endpoints like Product and Store, I
would still hand-code them for speed, but I'd add a small separate response object instead of
returning the database entity straight away, just so a database change can't accidentally break
the API without anyone realizing.

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?


While working through this assignment I actually ran into two real bugs, and both of them were
about concurrency and transactions, not basic logic. One let two updates to the same warehouse
overwrite each other instead of failing safely, and the other let an old system get notified
about a store even though the save had actually failed. The thing that stood out to me was that
every plain unit test was green the whole time. Those tests mock out the database, so they're
great for checking business rules, but they simply can't see a bug that only shows up when a
real database and a real transaction are involved. That experience is basically how I'd rank
things for this project.

I'd still start with plenty of unit tests for the business rules, like "capacity can't go over
the limit" or "you can't archive something twice." They're quick to write, quick to run, and
they catch obvious logic mistakes early, so there's no reason to skip them.

After that, I'd make sure there are integration tests that go through a real database and a real
transaction, even if it's a lightweight one like H2 for local runs. This is the level that
actually caught both bugs above, so I wouldn't treat it as optional just because it's slower
than a unit test.

Then I'd add a few tests specifically for concurrency, where multiple threads try to do
something at the same time. I wouldn't put these everywhere though, only on the parts where two
people could genuinely touch the same data at once, like warehouse stock or archiving a
warehouse, since these tests are slower and can get flaky if you're not careful with them.

Parameterized tests are a nice cheap way to cover a bunch of edge cases, like capacity exactly at
the limit versus one above it, without writing a separate test method for each case.

And finally, a small number of end-to-end tests that hit the actual REST endpoints, just to
check everything is wired together correctly. I'd keep this to the most important flows though,
not as a replacement for the layers above.

To keep this useful over time instead of just chasing a percentage, I'd use the coverage report
as a way to ask questions rather than a number to maximize. If I see an uncovered line inside a
use case or repository, I'd ask myself "could two people touching this at the same time cause a
problem, and would we actually notice?" That question is pretty much how the two real bugs in
this project got found in the first place.
