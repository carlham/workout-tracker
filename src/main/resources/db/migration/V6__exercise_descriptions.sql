-- description: how to perform the exercise. comment: notes about it.
ALTER TABLE "exercises" ADD COLUMN "description" text;

UPDATE exercises e
SET description = v.description
FROM (VALUES
  ('Barbell Bench Press',        'Pinch shoulder blades back, lower the bar to mid-chest with elbows about 45 degrees from your body.'),
  ('Incline Dumbbell Press',     'Bench at 30-45 degrees. Lower the dumbbells to your upper chest, press up and slightly in.'),
  ('Dumbbell Fly',               'Keep a slight bend in the elbows and lower in a wide arc until you feel a stretch in your chest.'),
  ('Cable Crossover',            'Lean slightly forward and bring the handles together in a hugging arc. Squeeze your chest.'),
  ('Push-Up',                    'Hands under shoulders, body in a straight line. Lower until your chest nearly touches the floor.'),
  ('Chest Dip',                  'Lean forward and lower until your shoulders are just below your elbows, then press back up.'),

  ('Deadlift',                   'Bar over mid-foot, flat back. Push the floor away and keep the bar close to your legs.'),
  ('Pull-Up',                    'Overhand grip. Pull your chest toward the bar, then lower under control to straight arms.'),
  ('Chin-Up',                    'Underhand, shoulder-width grip. Pull your chin over the bar and lower slowly.'),
  ('Barbell Row',                'Hinge to about 45 degrees with a flat back and pull the bar to your lower ribs.'),
  ('One-Arm Dumbbell Row',       'Brace on a bench and pull the dumbbell toward your hip without twisting your torso.'),
  ('Lat Pulldown',               'Lean back slightly and pull the bar to your upper chest by driving your elbows down.'),
  ('Seated Cable Row',           'Chest up. Pull the handle to your stomach and squeeze your shoulder blades together.'),

  ('Overhead Press',             'Brace your glutes and core. Press the bar straight up and move your head through at the top.'),
  ('Dumbbell Shoulder Press',    'Start with the dumbbells at ear height and press up without arching your lower back.'),
  ('Lateral Raise',              'Slight bend in the elbows. Raise to shoulder height leading with the elbows, lower slowly.'),
  ('Rear Delt Fly',              'Hinge forward and raise the weights out to the sides, squeezing the back of your shoulders.'),
  ('Face Pull',                  'Rope at upper-chest height. Pull toward your face with elbows high and hands rotating back.'),

  ('Barbell Curl',               'Keep your elbows fixed at your sides. Curl without swinging and lower under control.'),
  ('Dumbbell Curl',              'Elbows at your sides. Turn your palms up as you curl and lower slowly.'),
  ('Hammer Curl',                'Palms facing each other the whole time, elbows at your sides.'),
  ('Preacher Curl',              'Upper arms flat on the pad. Lower until your arms are almost straight.'),

  ('Triceps Pushdown',           'Pin your elbows to your sides and push down until your arms are straight.'),
  ('Overhead Triceps Extension', 'Elbows pointing up and close to your head. Lower the weight behind your head, then extend.'),
  ('Skull Crusher',              'Lower the bar toward your forehead by bending only at the elbows, then extend.'),
  ('Close-Grip Bench Press',     'Hands about shoulder-width apart, elbows tucked. Lower the bar to your lower chest.'),

  ('Back Squat',                 'Bar on your upper back. Brace, squat to at least parallel and keep your knees tracking over your toes.'),
  ('Front Squat',                'Bar on the front of your shoulders with elbows high. Stay upright as you squat down.'),
  ('Leg Press',                  'Feet shoulder-width on the platform. Lower until your knees are near 90 degrees.'),
  ('Bulgarian Split Squat',      'Rear foot on a bench. Lower straight down until your front thigh is about parallel.'),
  ('Walking Lunge',              'Take long steps, lower your back knee toward the floor and push through your front heel.'),
  ('Leg Extension',              'Pad on your lower shins. Extend fully, then lower slowly.'),
  ('Romanian Deadlift',          'Soft knees. Push your hips back with a flat back until you feel a hamstring stretch.'),
  ('Lying Leg Curl',             'Keep your hips pressed into the pad and curl your heels toward your glutes.'),
  ('Hip Thrust',                 'Upper back on a bench. Drive through your heels and squeeze your glutes at the top.'),
  ('Standing Calf Raise',        'Stretch fully at the bottom, rise onto the balls of your feet and pause at the top.'),
  ('Seated Calf Raise',          'Pad on your lower thighs. Lower your heels fully, press up and pause.'),

  ('Plank',                      'Elbows under shoulders, straight line from head to heels. Squeeze your glutes and abs.'),
  ('Hanging Leg Raise',          'Hang without swinging and raise your legs by curling your pelvis up.'),
  ('Cable Crunch',               'Kneel with the rope by your head. Crunch by curling your spine, not by pulling with your arms.'),
  ('Ab Wheel Rollout',           'Brace and roll out only as far as you can without your lower back sagging.')
) AS v(name, description)
WHERE e.name = v.name AND e.owner_id IS NULL;
