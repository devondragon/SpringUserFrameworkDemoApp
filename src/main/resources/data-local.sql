-- Test data for local development
-- Uses INSERT IGNORE to skip if data already exists

INSERT IGNORE INTO events (id, name, description, location, date, time)
VALUES
    (1, 'Tech Conference 2025', 'A conference about the latest in tech.', 'San Francisco, CA', '2025-06-15', '10:00:00'),
    (2, 'Spring Boot Workshop', 'Learn Spring Boot from experts.', 'New York, NY', '2025-07-10', '14:00:00'),
    (3, 'AI Symposium', 'Exploring the advancements in AI.', 'Los Angeles, CA', '2026-08-22', '09:30:00'),
    (4, 'DevOps Summit', 'Best practices for CI/CD pipelines, infrastructure as code, and cloud-native deployments.', 'Seattle, WA', '2026-03-18', '09:00:00'),
    (5, 'Kubernetes Deep Dive', 'Hands-on workshop covering advanced Kubernetes patterns and operators.', 'Austin, TX', '2026-04-05', '13:00:00'),
    (6, 'Java 25 Launch Party', 'Celebrating the release of Java 25 with demos and networking.', 'Denver, CO', '2026-05-12', '18:00:00'),
    (7, 'Security in the Cloud', 'Zero-trust architecture, secrets management, and compliance automation.', 'Boston, MA', '2026-06-20', '10:30:00'),
    (8, 'Microservices Architecture Forum', 'Patterns for building resilient distributed systems at scale.', 'Chicago, IL', '2026-07-08', '11:00:00'),
    (9, 'Open Source Contributor Day', 'Learn how to contribute to popular open source projects with guided mentorship.', 'Portland, OR', '2026-09-14', '09:00:00'),
    (10, 'Database Performance Tuning', 'Query optimization, indexing strategies, and monitoring for SQL and NoSQL databases.', 'Atlanta, GA', '2026-10-03', '14:00:00'),
    (11, 'Frontend Frameworks Showdown', 'Comparing React, Vue, Angular, and Svelte with live coding demos.', 'Miami, FL', '2026-11-15', '10:00:00'),
    (12, 'API Design Masterclass', 'RESTful best practices, GraphQL patterns, and API versioning strategies.', 'Philadelphia, PA', '2027-01-22', '13:30:00'),
    (13, 'Machine Learning in Production', 'MLOps practices for deploying and monitoring ML models at scale.', 'San Diego, CA', '2027-02-28', '09:00:00'),
    (14, 'Startup Tech Meetup', 'Networking event for tech founders and engineers building the next big thing.', 'Nashville, TN', '2027-03-10', '17:30:00'),
    (15, 'Women in Tech Summit', 'Inspiring talks and workshops celebrating women in the technology industry.', 'Washington, DC', '2027-04-25', '08:30:00');
